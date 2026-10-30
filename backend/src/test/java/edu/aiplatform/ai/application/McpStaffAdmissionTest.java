package edu.aiplatform.ai.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

import edu.aiplatform.ai.domain.*;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.shared.error.ApiException;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class McpStaffAdmissionTest {
    private final Instant now = Instant.parse("2026-10-26T10:00:00Z");
    private final McpCredentialRepository repository = mock(McpCredentialRepository.class);
    private final IAccountLookup accounts = mock(IAccountLookup.class);
    private final IAuthorization authorization = mock(IAuthorization.class);
    private final IAudit audit = mock(IAudit.class);
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private final McpCredentialService service = new McpCredentialService(repository, accounts,
            new AiAccess(authorization, audit, clock), audit, clock,
            new KeyCipher(KeyCipherAndCatalogTest.properties(KeyCipherAndCatalogTest.KEY_V1, null)));

    @Test void studentManagesCredentialsWithMcpPermissionWithoutSharedAi() {
        Actor admin = new Actor(UUID.randomUUID(), Role.STUDENT);
        when(authorization.authorize(eq(admin), anyString(), any())).thenAnswer(call ->
                "MCP_SELF".equals(call.getArgument(1)) ? AuthorizationDecision.allow()
                        : AuthorizationDecision.deny("ROLE"));
        assertThat(service.create(admin, "Reporting", McpCredential.Access.READ_ONLY, 7).token()).startsWith("mcp_");
        service.list(admin);
        verify(authorization, times(2)).authorize(eq(admin), eq("MCP_SELF"), any());
    }

    @Test void adminCannotManageCredentialsDespiteManagingInstitutionalAi() {
        Actor student = new Actor(UUID.randomUUID(), Role.ADMIN);
        when(authorization.authorize(eq(student), anyString(), any())).thenAnswer(call ->
                "AI_ADMIN".equals(call.getArgument(1)) ? AuthorizationDecision.allow()
                        : AuthorizationDecision.deny("ROLE"));
        assertThatThrownBy(() -> service.create(student, "x", McpCredential.Access.READ_ONLY, 7)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.list(student)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.revoke(student, UUID.randomUUID())).isInstanceOf(ApiException.class);
        verify(repository, never()).insert(any());
    }

    @Test void oldTokenIsRejectedAsSoonAsCurrentAccountRoleBecomesAdmin() {
        UUID accountId = UUID.randomUUID();
        String token = "mcp_existing-test-secret";
        var credential = new McpCredential(UUID.randomUUID(), accountId, "test", McpCredential.Access.READ_WRITE,
                McpCredentialService.hash(token), "cret", now.plusSeconds(60), null, null, now);
        when(repository.findByHash(credential.tokenHash())).thenReturn(Optional.of(credential));
        when(accounts.getContact(accountId)).thenReturn(Optional.of(new AccountContact(accountId, "test@local", "test",
                Role.TEACHER, AccountStatus.ACTIVE)), Optional.of(new AccountContact(accountId, "test@local", "test",
                Role.ADMIN, AccountStatus.ACTIVE)), Optional.of(new AccountContact(accountId, "test@local", "test",
                Role.STUDENT, AccountStatus.ACTIVE)));
        assertThat(service.authenticate(token)).isPresent();
        assertThat(service.authenticate(token)).isEmpty();
        assertThat(service.authenticate(token)).get().extracting(p -> p.actor().role()).isEqualTo(Role.STUDENT);
    }
}
