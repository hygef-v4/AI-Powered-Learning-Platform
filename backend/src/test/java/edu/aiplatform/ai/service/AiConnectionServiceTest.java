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
        when(repository.update(any())).thenReturn(true);
    }

    @Test
    void adminHasNoPersonalConnection() {
        assertThatThrownBy(() -> service.listPersonal(admin))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.status()).isEqualTo(HttpStatus.NOT_FOUND));
        ArgumentCaptor<AuditEvent> event = ArgumentCaptor.forClass(AuditEvent.class);
        verify(audit).record(event.capture());
        assertThat(event.getValue().action()).isEqualTo("ACCESS_DENIED");
    }

    @Test
    void newKeyIsEncryptedAndOnlyLastFourCharactersAreShownOrAudited() {
        var view = service.savePersonal(teacher, AiProvider.GEMINI, new SaveCommand(" AIzaSyD-secret-3kQf ",
                "gemini-2.5-flash", null));

        ArgumentCaptor<AiConnection> saved = ArgumentCaptor.forClass(AiConnection.class);
        verify(repository).insert(saved.capture());
        assertThat(saved.getValue().keyCiphertext()).isNotEmpty();
        assertThat(new String(saved.getValue().keyCiphertext())).doesNotContain("secret");
        assertThat(cipher.decrypt(saved.getValue().encryptedKey())).isEqualTo("AIzaSyD-secret-3kQf");
        assertThat(view.keyHint()).isEqualTo("3kQf");
        assertThat(view.toString()).doesNotContain("secret");
        ArgumentCaptor<AuditEvent> event = ArgumentCaptor.forClass(AuditEvent.class);
        verify(audit).record(event.capture());
        assertThat(event.getValue().details().toString()).doesNotContain("secret").contains("3kQf");
    }

    @Test
    void modelOutsideTheAllowedListAndBadKeysAreRejected() {
        assertThatThrownBy(() -> service.savePersonal(teacher, AiProvider.GEMINI, new SaveCommand("AIzaSy-key-123",
                "gemini-ultra", null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("AI_MODEL_NOT_ALLOWED"));
        assertThatThrownBy(() -> service.savePersonal(teacher, AiProvider.GEMINI, new SaveCommand("short",
                "gemini-2.5-flash", null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("AI_KEY_INVALID"));
        assertThatThrownBy(() -> service.savePersonal(teacher, AiProvider.GEMINI, new SaveCommand(null,
                "gemini-2.5-flash", null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("AI_KEY_REQUIRED"));
        verify(repository, never()).insert(any());
    }

    @Test
    void replacingNeedsMatchingVersion() {
        AiConnection existing = existing(AiConnection.Scope.PERSONAL);
        when(repository.findPersonal(teacher.accountId(), AiProvider.GEMINI)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.savePersonal(teacher, AiProvider.GEMINI, new SaveCommand("AIzaSy-new-key-9999",
                "gemini-2.5-pro", 9L)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.status()).isEqualTo(HttpStatus.CONFLICT));

        service.savePersonal(teacher, AiProvider.GEMINI, new SaveCommand(null, "gemini-2.5-pro", 2L));
        ArgumentCaptor<AiConnection> updated = ArgumentCaptor.forClass(AiConnection.class);
        verify(repository).update(updated.capture());
        assertThat(updated.getValue().model()).isEqualTo("gemini-2.5-pro");
        assertThat(updated.getValue().keyCiphertext()).isEqualTo(existing.keyCiphertext());
    }

    @Test
    void revokeDropsCiphertext() {
        when(repository.findPersonal(teacher.accountId(), AiProvider.GEMINI))
                .thenReturn(Optional.of(existing(AiConnection.Scope.PERSONAL)));

        service.revokePersonal(teacher, AiProvider.GEMINI);

        ArgumentCaptor<AiConnection> updated = ArgumentCaptor.forClass(AiConnection.class);
        verify(repository).update(updated.capture());
        assertThat(updated.getValue().status()).isEqualTo(Status.REVOKED);
        assertThat(updated.getValue().keyCiphertext()).isNull();
    }

    @Test
    void connectionTestUsesFixedPromptAndRecordsSanitizedResult() {
        when(repository.findPersonal(teacher.accountId(), AiProvider.GEMINI))
                .thenReturn(Optional.of(existing(AiConnection.Scope.PERSONAL)));
        when(gateway.call(eq(AiProvider.GEMINI), any())).thenThrow(new ProviderException(
                IProviderGateway.FailureKind.REJECTED, 401, "Key API không hợp lệ hoặc không có quyền (HTTP 401)", null));

        var view = service.testPersonal(teacher, AiProvider.GEMINI);

        assertThat(view.lastTestResult()).contains("401");
        ArgumentCaptor<IProviderGateway.Request> request = ArgumentCaptor.forClass(IProviderGateway.Request.class);
        verify(gateway).call(eq(AiProvider.GEMINI), request.capture());
        assertThat(request.getValue().maxOutputTokens()).isEqualTo(5);
        assertThat(request.getValue().user()).isEqualTo(ConnectionTestService.USER);
        assertThat(request.getValue().apiKey()).isEqualTo("AIzaSy-old-key-0000");
        ArgumentCaptor<AiUsageRecord> usage = ArgumentCaptor.forClass(AiUsageRecord.class);
        verify(usageRepository).insert(usage.capture());
        assertThat(usage.getValue().inputTokens()).as("không biết thì để rỗng, không ghi 0").isNull();
    }

    @Test
    void institutionalSwitchKeepsKey() {
        AiConnection shared = existing(AiConnection.Scope.INSTITUTIONAL);
        when(repository.findById(shared.id())).thenReturn(Optional.of(shared));
        when(gateway.call(any(), any())).thenReturn(new Response("OK", "r", null));

        service.setInstitutionalEnabled(admin, shared.id(), false, 2L);

        ArgumentCaptor<AiConnection> updated = ArgumentCaptor.forClass(AiConnection.class);
        verify(repository).update(updated.capture());
        assertThat(updated.getValue().status()).isEqualTo(Status.DISABLED);
        assertThat(updated.getValue().keyCiphertext()).isNotNull();
    }

    @Test
    void adminRevealsSavedSchoolKeyAndAuditsIt() {
        AiConnection shared = existing(AiConnection.Scope.INSTITUTIONAL);
        when(repository.findById(shared.id())).thenReturn(Optional.of(shared));

        assertThat(service.revealInstitutional(admin, shared.id())).isEqualTo("AIzaSy-old-key-0000");
        ArgumentCaptor<edu.aiplatform.audit.contract.AuditEvent> event =
                ArgumentCaptor.forClass(edu.aiplatform.audit.contract.AuditEvent.class);
        verify(audit, org.mockito.Mockito.atLeastOnce()).record(event.capture());
        assertThat(event.getAllValues()).extracting(edu.aiplatform.audit.contract.AuditEvent::action)
                .contains("AI_CONNECTION_KEY_REVEALED");
        assertThat(event.getAllValues().toString()).doesNotContain("AIzaSy-old-key-0000");
    }

    @Test
    void institutionalRevealAllowsDisabledButNeverPersonalRevokedOrMissing() {
        AiConnection shared=existing(AiConnection.Scope.INSTITUTIONAL);
        when(repository.findById(shared.id())).thenReturn(Optional.of(shared.withStatus(Status.DISABLED,NOW)));
        assertThat(service.revealInstitutional(admin,shared.id())).isEqualTo("AIzaSy-old-key-0000");
        when(repository.findById(shared.id())).thenReturn(Optional.of(shared.revoked(NOW)));
        assertThatThrownBy(()->service.revealInstitutional(admin,shared.id())).isInstanceOf(ApiException.class);
        var personal=existing(AiConnection.Scope.PERSONAL);
        when(repository.findById(personal.id())).thenReturn(Optional.of(personal));
        assertThatThrownBy(()->service.revealInstitutional(admin,personal.id())).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->service.revealInstitutional(admin,UUID.randomUUID())).isInstanceOf(ApiException.class);
    }

    @Test
    void draftTestUsesTypedKeyOrSavedKeyWithoutSavingResult() {
        AiConnection shared = existing(AiConnection.Scope.INSTITUTIONAL);
        when(repository.findById(shared.id())).thenReturn(Optional.of(shared));
        when(gateway.call(any(), any())).thenReturn(new Response("OK", "r", null));

        var typed = service.testInstitutionalDraft(admin, AiProvider.GEMINI, null,
                new SaveCommand("AIzaSy-new-key-9999", "gemini-2.5-flash", null));
        var saved = service.testInstitutionalDraft(admin, AiProvider.GEMINI, shared.id(),
                new SaveCommand(null, "gemini-2.5-flash", null));

        assertThat(typed.ok()).isTrue();
        assertThat(saved.ok()).isTrue();
        ArgumentCaptor<IProviderGateway.Request> request = ArgumentCaptor.forClass(IProviderGateway.Request.class);
        verify(gateway, org.mockito.Mockito.times(2)).call(eq(AiProvider.GEMINI), request.capture());
        assertThat(request.getAllValues()).extracting(IProviderGateway.Request::apiKey)
                .containsExactly("AIzaSy-new-key-9999", "AIzaSy-old-key-0000");
        verify(repository, org.mockito.Mockito.never()).saveTest(any(), any(), any());
        verify(repository, org.mockito.Mockito.never()).insert(any());
        assertThatThrownBy(() -> service.testInstitutionalDraft(admin, AiProvider.GEMINI, null,
                new SaveCommand(" ", "gemini-2.5-flash", null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("AI_KEY_REQUIRED"));
    }

    private AiConnection existing(AiConnection.Scope scope) {
        AiConnection created = AiConnection.create(UUID.randomUUID(), scope,
                scope == AiConnection.Scope.PERSONAL ? teacher.accountId() : null, AiProvider.GEMINI, "gemini-2.5-flash",
                cipher.encrypt("AIzaSy-old-key-0000"), "0000", NOW);
        return new AiConnection(created.id(), created.scope(), created.ownerId(), created.provider(), created.model(),
                created.keyCiphertext(), created.keyIv(), created.keyVersion(), created.keyHint(), created.status(), null,
                null, NOW, NOW, 2);
    }
}
