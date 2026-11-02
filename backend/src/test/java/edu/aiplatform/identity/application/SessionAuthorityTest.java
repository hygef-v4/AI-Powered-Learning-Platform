package edu.aiplatform.identity.application;

import static edu.aiplatform.identity.IdentityTestData.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import edu.aiplatform.identity.IdentityTestData;
import edu.aiplatform.identity.contract.AccountStatus;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.identity.domain.Account;
import edu.aiplatform.identity.domain.AccountRepository;
import edu.aiplatform.identity.infrastructure.JwtService;
import edu.aiplatform.identity.infrastructure.SessionStore;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SessionAuthorityTest {

    private final JwtService jwt = new JwtService(IdentityTestData.properties(), Clock.fixed(NOW, ZoneOffset.UTC));
    private final SessionStore sessions = mock(SessionStore.class);
    private final AccountRepository accounts = mock(AccountRepository.class);
    private final SessionAuthority authority = new SessionAuthority(jwt, sessions, accounts);

    private SessionStore.SessionRecord sessionOf(UUID accountId) {
        return new SessionStore.SessionRecord(accountId, NOW, NOW, "hash");
    }

    @Test
    void validTokenGivesActorWithRoleFromDatabase() {
        Account account = IdentityTestData.account(Role.TEACHER, AccountStatus.ACTIVE, "h");
        when(sessions.find("sid")).thenReturn(Optional.of(sessionOf(account.id())));
        when(accounts.findById(account.id())).thenReturn(Optional.of(account));

        // Token còn ghi STUDENT nhưng role hiện hành là TEACHER (NFR-U01-04).
        String token = jwt.issue(account.id(), Role.STUDENT, "sid");

        assertThat(authority.authenticate(token)).hasValueSatisfying(s -> {
            assertThat(s.actor().role()).isEqualTo(Role.TEACHER);
            assertThat(s.sid()).isEqualTo("sid");
        });
    }

    @Test
    void revokedSessionIsRejectedBeforeTokenExpires() {
        Account account = IdentityTestData.account(Role.STUDENT, AccountStatus.ACTIVE, "h");
        when(sessions.find("sid")).thenReturn(Optional.empty());

        assertThat(authority.authenticate(jwt.issue(account.id(), Role.STUDENT, "sid"))).isEmpty();
    }

    @Test
    void sessionOfAnotherAccountIsRejected() {
        Account account = IdentityTestData.account(Role.STUDENT, AccountStatus.ACTIVE, "h");
        when(sessions.find("sid")).thenReturn(Optional.of(sessionOf(UUID.randomUUID())));

        assertThat(authority.authenticate(jwt.issue(account.id(), Role.STUDENT, "sid"))).isEmpty();
    }

    @Test
    void disabledAccountIsRejected() {
        Account account = IdentityTestData.account(Role.STUDENT, AccountStatus.DISABLED, "h");
        when(sessions.find("sid")).thenReturn(Optional.of(sessionOf(account.id())));
        when(accounts.findById(account.id())).thenReturn(Optional.of(account));

        assertThat(authority.authenticate(jwt.issue(account.id(), Role.STUDENT, "sid"))).isEmpty();
    }

    @Test
    void garbageTokenIsRejectedWithoutTouchingRedis() {
        assertThat(authority.authenticate("rac")).isEmpty();
    }
}
