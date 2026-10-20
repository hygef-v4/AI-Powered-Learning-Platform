package edu.aiplatform.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.aiplatform.ai.domain.McpCredential;
import edu.aiplatform.ai.domain.McpCredential.Access;
import edu.aiplatform.ai.domain.McpCredentialRepository;
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
            new AiAccess(authorization, audit, clock), audit, clock);
    private final Actor student = new Actor(UUID.randomUUID(), Role.STUDENT);

    @Test
    void createShowsTokenOnceAndStoresOnlyItsHash() {
        when(authorization.authorize(eq(student), any(), any())).thenReturn(AuthorizationDecision.allow());

        var created = service.create(student, "  Claude Desktop ", Access.READ_ONLY, null);

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
    void createRejectsBadInputAndSixthActiveCredential() {
        when(authorization.authorize(eq(student), any(), any())).thenReturn(AuthorizationDecision.allow());
        assertThatThrownBy(() -> service.create(student, "x", Access.READ_ONLY, 14))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("MCP_EXPIRY_INVALID"));
        assertThatThrownBy(() -> service.create(student, " ", Access.READ_ONLY, 7))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("MCP_LABEL_INVALID"));
        when(repository.countActive(student.accountId(), NOW)).thenReturn(5);
        assertThatThrownBy(() -> service.create(student, "x", Access.READ_WRITE, 90))
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
        when(accounts.getContact(student.accountId())).thenReturn(Optional.of(
                new AccountContact(student.accountId(), "a@x", "A", Role.TEACHER, AccountStatus.ACTIVE)));

        var principal = service.authenticate(TOKEN).orElseThrow();

        assertThat(principal.actor().role()).isEqualTo(Role.TEACHER);
        verify(repository).touch(credential.id(), NOW);
    }

    @Test
    void authenticateRejectsExpiredRevokedInactiveAndAdmin() {
        when(accounts.getContact(student.accountId())).thenReturn(Optional.of(
                new AccountContact(student.accountId(), "a@x", "A", Role.STUDENT, AccountStatus.ACTIVE)));
        when(repository.findByHash(any())).thenReturn(Optional.of(credential(NOW, null)));
        assertThat(service.authenticate(TOKEN)).isEmpty();
        when(repository.findByHash(any())).thenReturn(Optional.of(credential(NOW.plusSeconds(60), NOW)));
        assertThat(service.authenticate(TOKEN)).isEmpty();

        when(repository.findByHash(any())).thenReturn(Optional.of(credential(NOW.plusSeconds(60), null)));
        when(accounts.getContact(student.accountId())).thenReturn(Optional.of(
                new AccountContact(student.accountId(), "a@x", "A", Role.STUDENT, AccountStatus.DISABLED)));
        assertThat(service.authenticate(TOKEN)).isEmpty();
        when(accounts.getContact(student.accountId())).thenReturn(Optional.of(
                new AccountContact(student.accountId(), "a@x", "A", Role.ADMIN, AccountStatus.ACTIVE)));
        assertThat(service.authenticate(TOKEN)).isEmpty();

        assertThat(service.authenticate("not-a-token")).isEmpty();
        assertThat(service.authenticate(null)).isEmpty();
        verify(repository, never()).touch(any(), any());
    }

    @Test
    void revokeChecksOwnerAndAuditsOnce() {
        when(authorization.authorize(eq(student), any(), any())).thenReturn(AuthorizationDecision.allow());
        McpCredential credential = credential(NOW.plusSeconds(60), null);
        when(repository.findOwn(student.accountId(), credential.id())).thenReturn(Optional.of(credential));
        when(repository.revoke(credential.id(), NOW)).thenReturn(1);

        service.revoke(student, credential.id());

        ArgumentCaptor<AuditEvent> event = ArgumentCaptor.forClass(AuditEvent.class);
        verify(audit).record(event.capture());
        assertThat(event.getValue().action()).isEqualTo("MCP_REVOKED");
        UUID other = UUID.randomUUID();
        when(repository.findOwn(student.accountId(), other)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.revoke(student, other))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo(AiAccess.NOT_FOUND));
    }

    private static final String TOKEN = "mcp_abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG";

    private McpCredential credential(Instant expiresAt, Instant revokedAt) {
        return new McpCredential(UUID.randomUUID(), student.accountId(), "agent", Access.READ_ONLY,
                McpCredentialService.hash(TOKEN), "DEFG", expiresAt, revokedAt, null, NOW.minusSeconds(3600));
    }
}
