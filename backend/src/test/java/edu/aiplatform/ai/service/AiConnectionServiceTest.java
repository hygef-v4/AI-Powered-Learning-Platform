package edu.aiplatform.ai.service;

import edu.aiplatform.ai.config.AiProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.aiplatform.ai.service.AiConnectionService.SaveCommand;
import edu.aiplatform.ai.service.IProviderGateway.ProviderException;
import edu.aiplatform.ai.service.IProviderGateway.Response;
import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.ai.model.AiConnection;
import edu.aiplatform.ai.model.AiConnection.Status;
import edu.aiplatform.ai.repository.AiConnectionRepository;
import edu.aiplatform.ai.model.AiUsageRecord;
import edu.aiplatform.ai.repository.AiUsageRepository;
import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.AuthorizationDecision;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

class AiConnectionServiceTest {

    static final Instant NOW = Instant.parse("2026-10-21T03:00:00Z");
    private final AiConnectionRepository repository = mock(AiConnectionRepository.class);
    private final AiUsageRepository usageRepository = mock(AiUsageRepository.class);
    private final IProviderGateway gateway = mock(IProviderGateway.class);
    private final IAuthorization authorization = mock(IAuthorization.class);
    private final IAudit audit = mock(IAudit.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final AiProperties properties = KeyCipherAndCatalogTest.properties(KeyCipherAndCatalogTest.KEY_V1, null);
    private final KeyCipher cipher = new KeyCipher(properties);
    private final AiAccess access = new AiAccess(authorization, audit, clock);
    private final AiConnectionService service = new AiConnectionService(repository, cipher, new ProviderCatalog(properties),
            new ConnectionTestService(gateway, cipher, properties), new AiUsageService(usageRepository, access, clock),
            access, audit, clock);
    private final Actor teacher = new Actor(UUID.randomUUID(), Role.TEACHER);
    private final Actor admin = new Actor(UUID.randomUUID(), Role.ADMIN);

    @BeforeEach
    void setUp() {
        when(authorization.authorize(eq(teacher), any(), any())).thenReturn(AuthorizationDecision.allow());
        when(authorization.authorize(eq(admin), eq(Actions.AI_ADMIN), any())).thenReturn(AuthorizationDecision.allow());
        when(authorization.authorize(eq(admin), eq(Actions.AI_PERSONAL), any()))
                .thenReturn(AuthorizationDecision.deny("ROLE"));
        when(repository.findPersonal(any(), any())).thenReturn(Optional.empty());
        when(repository.updateConnection(any())).thenReturn(true);
    }

    @Test
    void adminHasNoPersonalConnection() {
        assertThatThrownBy(() -> service.listPersonal(admin))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.readStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        ArgumentCaptor<AuditEvent> event = ArgumentCaptor.forClass(AuditEvent.class);
    }

    @Test
    void newKeyIsEncryptedAndOnlyLastFourCharactersAreShownOrAudited() {
        var view = service.addPersonal(teacher, AiProvider.GEMINI, new SaveCommand(" AIzaSyD-secret-3kQf ",
                "gemini-2.5-flash", null));

        ArgumentCaptor<AiConnection> saved = ArgumentCaptor.forClass(AiConnection.class);
        verify(repository).insert(saved.capture());
        assertThat(saved.getValue().keyCiphertext()).isNotEmpty();
        assertThat(new String(saved.getValue().keyCiphertext())).doesNotContain("secret");
        assertThat(cipher.decryptConnectionKey(saved.getValue())).isEqualTo("AIzaSyD-secret-3kQf");
        assertThat(view.keyHint()).isEqualTo("3kQf");
        assertThat(view.toString()).doesNotContain("secret");
        ArgumentCaptor<AuditEvent> event = ArgumentCaptor.forClass(AuditEvent.class);
    }

    @Test
    void modelOutsideTheAllowedListAndBadKeysAreRejected() {
        assertThatThrownBy(() -> service.addPersonal(teacher, AiProvider.GEMINI, new SaveCommand("AIzaSy-key-123",
                "gemini-ultra", null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.readCode()).isEqualTo("AI_MODEL_NOT_ALLOWED"));
        assertThatThrownBy(() -> service.addPersonal(teacher, AiProvider.GEMINI, new SaveCommand("short",
                "gemini-2.5-flash", null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.readCode()).isEqualTo("AI_KEY_INVALID"));
        assertThatThrownBy(() -> service.addPersonal(teacher, AiProvider.GEMINI, new SaveCommand(null,
                "gemini-2.5-flash", null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.readCode()).isEqualTo("AI_KEY_REQUIRED"));
        verify(repository, never()).insert(any());
    }

    @Test
    void replacingNeedsMatchingVersion() {
        AiConnection existing = existing(AiConnection.Scope.PERSONAL);
        when(repository.findById(existing.id())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.replacePersonal(teacher, existing.id(), new SaveCommand("AIzaSy-new-key-9999",
                "gemini-2.5-pro", 9L)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.readStatus()).isEqualTo(HttpStatus.CONFLICT));

        service.replacePersonal(teacher, existing.id(), new SaveCommand(null, "gemini-2.5-pro", 2L));
        ArgumentCaptor<AiConnection> updated = ArgumentCaptor.forClass(AiConnection.class);
        verify(repository).updateConnection(updated.capture());
        assertThat(updated.getValue().model()).isEqualTo("gemini-2.5-pro");
        assertThat(updated.getValue().keyCiphertext()).isEqualTo(existing.keyCiphertext());
    }

    @Test
    void revokeDropsCiphertext() {
        AiConnection mine = existing(AiConnection.Scope.PERSONAL);
        when(repository.findById(mine.id())).thenReturn(Optional.of(mine));

        service.revokePersonal(teacher, mine.id());

        ArgumentCaptor<AiConnection> updated = ArgumentCaptor.forClass(AiConnection.class);
        verify(repository).updateConnection(updated.capture());
        assertThat(updated.getValue().status()).isEqualTo(Status.REVOKED);
        assertThat(updated.getValue().keyCiphertext()).isNull();
    }

    @Test
    void connectionTestUsesFixedPromptAndRecordsSanitizedResult() {
        AiConnection mine = existing(AiConnection.Scope.PERSONAL);
        when(repository.findById(mine.id())).thenReturn(Optional.of(mine));
        when(gateway.call(eq(AiProvider.GEMINI), any())).thenThrow(new ProviderException(
                IProviderGateway.FailureKind.REJECTED, 401, "Key API không hợp lệ hoặc không có quyền (HTTP 401)", null));

        var view = service.testPersonal(teacher, mine.id());

        assertThat(view.lastTestResult()).contains("401");
        ArgumentCaptor<IProviderGateway.Request> request = ArgumentCaptor.forClass(IProviderGateway.Request.class);
        verify(gateway).call(eq(AiProvider.GEMINI), request.capture());
        assertThat(request.getValue().maxOutputTokens()).isEqualTo(1024);
        assertThat(request.getValue().user()).isEqualTo(ConnectionTestService.USER);
        assertThat(request.getValue().apiKey()).isEqualTo("AIzaSy-old-key-0000");
        verify(usageRepository, never()).insert(any());
    }

    @Test
    void draftCheckRecordsProviderMeasuredUsage() {
        when(gateway.call(eq(AiProvider.GEMINI), any()))
                .thenReturn(new Response("OK", "r", new IProviderGateway.Usage(9L, 2L, null, null)));

        assertThat(service.testPersonalDraft(teacher, AiProvider.GEMINI, null,
                new SaveCommand("AIzaSy-new-key-9999", "gemini-2.5-flash", null)).ok()).isTrue();

        ArgumentCaptor<AiUsageRecord> usage = ArgumentCaptor.forClass(AiUsageRecord.class);
        verify(usageRepository).insert(usage.capture());
        assertThat(usage.getValue().taskType()).isEqualTo("CONNECTION_TEST");
        assertThat(usage.getValue().inputTokens()).isEqualTo(9L);
        assertThat(usage.getValue().outputTokens()).isEqualTo(2L);
    }

    private void assertConnectionTestAudited() {
        ArgumentCaptor<AuditEvent> events = ArgumentCaptor.forClass(AuditEvent.class);
        verify(audit, org.mockito.Mockito.atLeastOnce()).record(events.capture());
        assertThat(events.getAllValues()).extracting(AuditEvent::action).contains("AI_CONNECTION_TESTED");
    }

    @Test
    void truncatedProbeStoresCanonicalSuccessAndProviderUsage() {
        AiConnection mine = existing(AiConnection.Scope.PERSONAL);
        when(repository.findById(mine.id())).thenReturn(Optional.of(mine));
        when(gateway.call(eq(AiProvider.GEMINI), any())).thenThrow(
                new IProviderGateway.OutputLimitException(new IProviderGateway.Usage(12L, 1024L, null, 1024L)));

        assertThat(service.testPersonal(teacher, mine.id()).lastTestResult()).isEqualTo("Đạt");

        verify(repository).saveTest(mine.id(), "Đạt", NOW);
        ArgumentCaptor<AiUsageRecord> usage = ArgumentCaptor.forClass(AiUsageRecord.class);
        verify(usageRepository).insert(usage.capture());
        assertThat(usage.getValue().outcome()).isEqualTo("OK");
        assertThat(usage.getValue().inputTokens()).isEqualTo(12L);
        assertThat(usage.getValue().outputTokens()).isEqualTo(1024L);
        assertThat(usage.getValue().reasoningTokens()).isEqualTo(1024L);
    }

    @Test
    void replacedKeyStaysBoundToItsOwnConnection() {
        AiConnection personal = existing(AiConnection.Scope.PERSONAL);
        when(repository.findById(personal.id())).thenReturn(Optional.of(personal));

        service.replacePersonal(teacher, personal.id(), new SaveCommand("AIzaSy-new-key-7777", "gemini-2.5-flash", 2L));

        ArgumentCaptor<AiConnection> updated = ArgumentCaptor.forClass(AiConnection.class);
        verify(repository).updateConnection(updated.capture());
        assertThat(cipher.decryptConnectionKey(updated.getValue())).isEqualTo("AIzaSy-new-key-7777");
        AiConnection other = existing(AiConnection.Scope.PERSONAL);
        AiConnection copied = new AiConnection(other.id(), other.scope(), other.ownerId(), other.provider(),
                other.model(), updated.getValue().keyCiphertext(), updated.getValue().keyIv(),
                updated.getValue().keyVersion(), other.keyHint(), other.status(), null, null, NOW, NOW, 2);
        assertThatThrownBy(() -> cipher.decryptConnectionKey(copied)).isInstanceOf(IllegalStateException.class);
    }

    private AiConnection existing(AiConnection.Scope scope) {
        UUID id = UUID.randomUUID();
        UUID ownerId = scope == AiConnection.Scope.PERSONAL ? teacher.accountId() : null;
        AiConnection created = AiConnection.createConnection(id, scope, ownerId, AiProvider.GEMINI, "gemini-2.5-flash",
                cipher.encryptConnectionKey(id, scope, ownerId, AiProvider.GEMINI, "AIzaSy-old-key-0000"), "0000", NOW);
        return new AiConnection(created.id(), created.scope(), created.ownerId(), created.provider(), created.model(),
                created.keyCiphertext(), created.keyIv(), created.keyVersion(), created.keyHint(), created.status(), null,
                null, NOW, NOW, 2);
    }
}
