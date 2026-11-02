package edu.aiplatform.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.aiplatform.ai.model.McpCredential;
import edu.aiplatform.ai.model.McpCredential.Access;
import edu.aiplatform.ai.repository.McpCredentialRepository;
import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.identity.contract.AccountContact;
import edu.aiplatform.identity.contract.AccountStatus;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.AuthorizationDecision;
import edu.aiplatform.identity.contract.IAccountLookup;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class McpCredentialServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-21T03:00:00Z");
    private final McpCredentialRepository repository = mock(McpCredentialRepository.class);
    private final IAccountLookup accounts = mock(IAccountLookup.class);
    private final IAuthorization authorization = mock(IAuthorization.class);
    private final IAudit audit = mock(IAudit.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final McpCredentialService service = new McpCredentialService(repository, accounts,
            new AiAccess(authorization, audit, clock), audit, clock,
            new KeyCipher(KeyCipherAndCatalogTest.properties(KeyCipherAndCatalogTest.KEY_V1, null)));
    private final Actor staff = new Actor(UUID.randomUUID(), Role.TEACHER);

    @Test
    void createShowsTokenOnceAndStoresOnlyItsHash() {
        when(authorization.authorize(eq(staff), any(), any())).thenReturn(AuthorizationDecision.allow());

        var created = service.create(staff, "  Claude Desktop ", Access.READ_ONLY, null);

        ArgumentCaptor<McpCredential> saved = ArgumentCaptor.forClass(McpCredential.class);
        verify(repository).insert(saved.capture());
        String token = created.token();
        assertThat(token).startsWith("mcp_").hasSize(4 + 43);
        assertThat(saved.getValue().tokenHash()).isEqualTo(McpCredentialService.hash(token)).doesNotContain(token);
        assertThat(saved.getValue().tokenHint()).isEqualTo(token.substring(token.length() - 4));
        assertThat(saved.getValue().label()).isEqualTo("Claude Desktop");
        assertThat(saved.getValue().expiresAt()).isEqualTo(NOW.plus(Duration.ofDays(30)));
        assertThat(created.toString()).doesNotContain(token);
        ArgumentCaptor<AuditEvent> event = ArgumentCaptor.forClass(AuditEvent.class);
        verify(audit).record(event.capture());
        assertThat(event.getValue().action()).isEqualTo("MCP_CREDENTIAL_CREATED");
        assertThat(event.getValue().details().toString()).doesNotContain(token);
    }

    @Test
    void ownerRevealsEncryptedTokenAndEditsLabelAndAccess() {
        when(authorization.authorize(eq(staff), any(), any())).thenReturn(AuthorizationDecision.allow());
        var created = service.create(staff, "Claude Desktop", Access.READ_ONLY, 7);
        ArgumentCaptor<McpCredential> saved = ArgumentCaptor.forClass(McpCredential.class);
        verify(repository).insert(saved.capture());
        ArgumentCaptor<edu.aiplatform.ai.model.AiConnection.EncryptedKey> secret =
                ArgumentCaptor.forClass(edu.aiplatform.ai.model.AiConnection.EncryptedKey.class);
        verify(repository).saveSecret(eq(saved.getValue().id()), secret.capture());
        assertThat(new String(secret.getValue().ciphertext(), java.nio.charset.StandardCharsets.ISO_8859_1))
                .doesNotContain(created.token());
        assertThat(created.credential().revealable()).isTrue();

        UUID id = saved.getValue().id();
        when(repository.findOwn(staff.accountId(), id)).thenReturn(java.util.Optional.of(saved.getValue()));
        when(repository.findSecret(id)).thenReturn(java.util.Optional.of(secret.getValue()));

        assertThat(service.reveal(staff, id)).isEqualTo(created.token());
        var edited = service.update(staff, id, " Cursor ", Access.READ_WRITE);
        verify(repository).update(id, "Cursor", Access.READ_WRITE);
        assertThat(edited.label()).isEqualTo("Cursor");
        assertThat(edited.access()).isEqualTo(Access.READ_WRITE);
        ArgumentCaptor<AuditEvent> events = ArgumentCaptor.forClass(AuditEvent.class);
        verify(audit, org.mockito.Mockito.atLeast(3)).record(events.capture());
        assertThat(events.getAllValues()).extracting(AuditEvent::action)
                .contains("MCP_TOKEN_REVEALED", "MCP_CREDENTIAL_UPDATED");
        assertThat(events.getAllValues().toString()).doesNotContain(created.token());
    }

    @Test
    void createRejectsBadInputAndSixthActiveCredential() {
        when(authorization.authorize(eq(staff), any(), any())).thenReturn(AuthorizationDecision.allow());
        assertThatThrownBy(() -> service.create(staff, "x", Access.READ_ONLY, 14))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("MCP_EXPIRY_INVALID"));
        assertThatThrownBy(() -> service.create(staff, " ", Access.READ_ONLY, 7))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("MCP_LABEL_INVALID"));
        when(repository.countActive(staff.accountId(), NOW)).thenReturn(5);
        assertThatThrownBy(() -> service.create(staff, "x", Access.READ_WRITE, 90))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("MCP_LIMIT_REACHED"));
        verify(repository, never()).insert(any());
    }

    @Test
    void adminCannotCreateCredential() {
        Actor admin = new Actor(UUID.randomUUID(), Role.ADMIN);
        when(authorization.authorize(eq(admin), any(), any())).thenReturn(AuthorizationDecision.deny("ROLE"));

        assertThatThrownBy(() -> service.create(admin, "x", Access.READ_ONLY, 7)).isInstanceOf(ApiException.class);
        verify(repository, never()).insert(any());
    }

    @Test
    void authenticateAcceptsActiveCredentialWithCurrentRole() {
        McpCredential credential = credential(NOW.plusSeconds(60), null);
        when(repository.findByHash(credential.tokenHash())).thenReturn(Optional.of(credential));
        // role đổi sau khi tạo (Student → Teacher), mật khẩu đổi: credential vẫn dùng được với role hiện hành
        when(accounts.getContact(staff.accountId())).thenReturn(Optional.of(
                new AccountContact(staff.accountId(), "a@x", "A", Role.TEACHER, AccountStatus.ACTIVE)));

        var principal = service.authenticate(TOKEN).orElseThrow();

        assertThat(principal.actor().role()).isEqualTo(Role.TEACHER);
        verify(repository).touch(credential.id(), NOW);
    }

    @Test
    void authenticateRejectsExpiredRevokedInactiveAndAdmin() {
        when(accounts.getContact(staff.accountId())).thenReturn(Optional.of(
                new AccountContact(staff.accountId(), "a@x", "A", Role.ADMIN, AccountStatus.ACTIVE)));
        when(repository.findByHash(any())).thenReturn(Optional.of(credential(NOW, null)));
        assertThat(service.authenticate(TOKEN)).isEmpty();
        when(repository.findByHash(any())).thenReturn(Optional.of(credential(NOW.plusSeconds(60), NOW)));
        assertThat(service.authenticate(TOKEN)).isEmpty();

        when(repository.findByHash(any())).thenReturn(Optional.of(credential(NOW.plusSeconds(60), null)));
        when(accounts.getContact(staff.accountId())).thenReturn(Optional.of(
                new AccountContact(staff.accountId(), "a@x", "A", Role.ADMIN, AccountStatus.DISABLED)));
        assertThat(service.authenticate(TOKEN)).isEmpty();
        when(accounts.getContact(staff.accountId())).thenReturn(Optional.of(
                new AccountContact(staff.accountId(), "a@x", "A", Role.ADMIN, AccountStatus.ACTIVE)));
        assertThat(service.authenticate(TOKEN)).isEmpty();

        assertThat(service.authenticate("not-a-token")).isEmpty();
        assertThat(service.authenticate(null)).isEmpty();
        verify(repository, never()).touch(any(), any());
    }

    @Test
    void revokeChecksOwnerAndAuditsOnce() {
        when(authorization.authorize(eq(staff), any(), any())).thenReturn(AuthorizationDecision.allow());
        McpCredential credential = credential(NOW.plusSeconds(60), null);
        when(repository.findOwn(staff.accountId(), credential.id())).thenReturn(Optional.of(credential));
        when(repository.revoke(credential.id(), NOW)).thenReturn(1);

        service.revoke(staff, credential.id());

        ArgumentCaptor<AuditEvent> event = ArgumentCaptor.forClass(AuditEvent.class);
        verify(audit).record(event.capture());
        assertThat(event.getValue().action()).isEqualTo("MCP_REVOKED");
        UUID other = UUID.randomUUID();
        when(repository.findOwn(staff.accountId(), other)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.revoke(staff, other))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo(AiAccess.NOT_FOUND));
    }

    @Test
    void revealRejectsForeignExpiredRevokedAndMissingCiphertext() {
        when(authorization.authorize(eq(staff), any(), any())).thenReturn(AuthorizationDecision.allow());
        UUID foreign=UUID.randomUUID();
        assertThatThrownBy(()->service.reveal(staff,foreign)).isInstanceOf(ApiException.class);
        var expired=credential(NOW,null);
        when(repository.findOwn(staff.accountId(),expired.id())).thenReturn(Optional.of(expired));
        assertThatThrownBy(()->service.reveal(staff,expired.id())).isInstanceOf(ApiException.class);
        var revoked=credential(NOW.plusSeconds(60),NOW);
        when(repository.findOwn(staff.accountId(),revoked.id())).thenReturn(Optional.of(revoked));
        assertThatThrownBy(()->service.reveal(staff,revoked.id())).isInstanceOf(ApiException.class);
        var active=credential(NOW.plusSeconds(60),null);
        when(repository.findOwn(staff.accountId(),active.id())).thenReturn(Optional.of(active));
        when(repository.findSecret(active.id())).thenReturn(Optional.empty());
        assertThatThrownBy(()->service.reveal(staff,active.id())).isInstanceOf(ApiException.class);
        verify(audit,never()).record(org.mockito.ArgumentMatchers.argThat(e->e.action().equals("MCP_TOKEN_REVEALED")));
    }

    private static final String TOKEN = "mcp_abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG";

    private McpCredential credential(Instant expiresAt, Instant revokedAt) {
        return new McpCredential(UUID.randomUUID(), staff.accountId(), "agent", Access.READ_ONLY,
                McpCredentialService.hash(TOKEN), "DEFG", expiresAt, revokedAt, null, NOW.minusSeconds(3600));
    }
}
