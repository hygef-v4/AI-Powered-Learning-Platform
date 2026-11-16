package edu.aiplatform.identity.service;

import static edu.aiplatform.identity.IdentityTestData.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import edu.aiplatform.identity.IdentityTestData;
import edu.aiplatform.identity.contract.AccountStatus;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.identity.model.Account;
import edu.aiplatform.identity.repository.AccountRepository;
import edu.aiplatform.identity.integration.JwtService;
import edu.aiplatform.identity.integration.SessionStore;
import edu.aiplatform.identity.integration.FakeRedis;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class SessionAuthorityTest {

    private final JwtService jwt = new JwtService(IdentityTestData.properties(), Clock.fixed(NOW, ZoneOffset.UTC));
    private final SessionStore sessions = mock(SessionStore.class);
    private final AccountRepository accounts = mock(AccountRepository.class);
    private final SessionAuthority authority = new SessionAuthority(jwt, sessions, accounts);

    private SessionStore.SessionRecord sessionOf(UUID accountId) {
        return new SessionStore.SessionRecord(accountId, NOW, NOW, "hash", null);
    }

    @Test
    void legacySessionIssuedBeforePasswordChangeIsRejectedAndDeleted() {
        Account account = IdentityTestData.account(Role.STUDENT, AccountStatus.ACTIVE, "h");
        when(sessions.findSession("sid")).thenReturn(Optional.of(new SessionStore.SessionRecord(
                account.id(), NOW.minusMillis(1), NOW, "hash", null)));
        when(accounts.findById(account.id())).thenReturn(Optional.of(account));

        assertThat(authority.authenticate(jwt.issue(account.id(), Role.STUDENT, "sid"))).isEmpty();
        verify(sessions).deleteSession("sid");
    }

    @Test
    void oldStampIsRejectedEvenWhenIssuedAfterPasswordReset() {
        Account account = IdentityTestData.account(Role.STUDENT, AccountStatus.ACTIVE, "h");
        when(sessions.findSession("sid")).thenReturn(Optional.of(new SessionStore.SessionRecord(
                account.id(), NOW.plusSeconds(1), NOW, "hash", NOW.minusMillis(1).toEpochMilli())));
        when(accounts.findById(account.id())).thenReturn(Optional.of(account));

        assertThat(authority.authenticate(jwt.issue(account.id(), Role.STUDENT, "sid"))).isEmpty();
        verify(sessions).deleteSession("sid");
    }

    @ParameterizedTest
    @CsvSource({"0", "1"})
    void equalOrFutureStampIsAcceptedEvenWhenIssuedEarlier(long offset) {
        Account account = IdentityTestData.account(Role.STUDENT, AccountStatus.ACTIVE, "h");
        when(sessions.findSession("sid")).thenReturn(Optional.of(new SessionStore.SessionRecord(
                account.id(), NOW.minusSeconds(1), NOW, "hash", NOW.toEpochMilli() + offset)));
        when(accounts.findById(account.id())).thenReturn(Optional.of(account));

        assertThat(authority.authenticate(jwt.issue(account.id(), Role.STUDENT, "sid"))).isPresent();
    }

    @ParameterizedTest
    @CsvSource({"0", "1"})
    void legacySessionIssuedAtOrAfterPasswordChangeIsAccepted(long offset) {
        Account account = IdentityTestData.account(Role.STUDENT, AccountStatus.ACTIVE, "h");
        when(sessions.findSession("sid")).thenReturn(Optional.of(new SessionStore.SessionRecord(
                account.id(), NOW.plusMillis(offset), NOW, "hash", null)));
        when(accounts.findById(account.id())).thenReturn(Optional.of(account));

        assertThat(authority.authenticate(jwt.issue(account.id(), Role.STUDENT, "sid"))).isPresent();
    }

    @Test
    void nullAccountPasswordTimestampAcceptsLegacySession() {
        Account account = IdentityTestData.account(Role.STUDENT, AccountStatus.ACTIVE, null);
        when(sessions.findSession("sid")).thenReturn(Optional.of(sessionOf(account.id())));
        when(accounts.findById(account.id())).thenReturn(Optional.of(account));

        assertThat(authority.authenticate(jwt.issue(account.id(), Role.STUDENT, "sid"))).isPresent();
    }

    @ParameterizedTest
    @ValueSource(strings = {"foreign", "stale", "deleted"})
    void staleAuthenticationSnapshotCannotAcceptAnInvalidReplacement(String replacement) {
        Account account = IdentityTestData.account(Role.STUDENT, AccountStatus.ACTIVE, "h");
        var oldSession = new SessionStore.SessionRecord(account.id(), NOW, NOW,
                "hash", NOW.minusMillis(1).toEpochMilli());
        var liveSession = new SessionStore.SessionRecord(
                replacement.equals("foreign") ? UUID.randomUUID() : account.id(), NOW, NOW, "hash",
                replacement.equals("stale") ? NOW.minusMillis(1).toEpochMilli() : NOW.toEpochMilli());
        when(sessions.findSession("sid")).thenReturn(Optional.of(oldSession), replacement.equals("deleted")
                ? Optional.empty() : Optional.of(liveSession));
        when(accounts.findById(account.id())).thenReturn(Optional.of(account));

        assertThat(authority.authenticate(jwt.issue(account.id(), account.role(), "sid"))).isEmpty();
        verify(sessions, org.mockito.Mockito.times(2)).findSession("sid");
        verify(sessions).deleteSession("sid");
    }

    @Test
    void authenticationSurvivesRestampAfterSessionRead() {
        Account account = IdentityTestData.account(Role.STUDENT, AccountStatus.ACTIVE, "h");
        var realSessions = new SessionStore(new FakeRedis().template,
                JsonMapper.builder().findAndAddModules().build(), Clock.fixed(NOW, ZoneOffset.UTC),
                IdentityTestData.properties());
        var issued = realSessions.createSession(account.id(), NOW.minusMillis(1).toEpochMilli());
        when(accounts.findById(account.id())).thenAnswer(invocation -> {
            realSessions.restampSession(issued.sid(), NOW.toEpochMilli());
            return Optional.of(account);
        });
        var realAuthority = new SessionAuthority(jwt, realSessions, accounts);

        assertThat(realAuthority.authenticate(jwt.issue(account.id(), account.role(), issued.sid()))).isPresent();
        assertThat(realSessions.findSession(issued.sid())).isPresent();
    }

    @Test
    void validTokenGivesActorWithRoleFromDatabase() {
        Account account = IdentityTestData.account(Role.TEACHER, AccountStatus.ACTIVE, "h");
        when(sessions.findSession("sid")).thenReturn(Optional.of(sessionOf(account.id())));
        when(accounts.findById(account.id())).thenReturn(Optional.of(account));

        // Token còn ghi STUDENT nhưng role hiện hành là TEACHER (NFR-U01-04).
        String token = jwt.issue(account.id(), Role.STUDENT, "sid");

        assertThat(authority.authenticate(token)).hasValueSatisfying(s -> {
            assertThat(s.actor().role()).isEqualTo(Role.TEACHER);
            assertThat(s.sid()).isEqualTo("sid");
        });
        verify(sessions).findSession("sid");
    }

    @Test
    void revokedSessionIsRejectedBeforeTokenExpires() {
        Account account = IdentityTestData.account(Role.STUDENT, AccountStatus.ACTIVE, "h");
        when(sessions.findSession("sid")).thenReturn(Optional.empty());

        assertThat(authority.authenticate(jwt.issue(account.id(), Role.STUDENT, "sid"))).isEmpty();
    }

    @Test
    void sessionOfAnotherAccountIsRejected() {
        Account account = IdentityTestData.account(Role.STUDENT, AccountStatus.ACTIVE, "h");
        when(sessions.findSession("sid")).thenReturn(Optional.of(sessionOf(UUID.randomUUID())));

        assertThat(authority.authenticate(jwt.issue(account.id(), Role.STUDENT, "sid"))).isEmpty();
    }

    @Test
    void disabledAccountIsRejected() {
        Account account = IdentityTestData.account(Role.STUDENT, AccountStatus.DISABLED, "h");
        when(sessions.findSession("sid")).thenReturn(Optional.of(sessionOf(account.id())));
        when(accounts.findById(account.id())).thenReturn(Optional.of(account));

        assertThat(authority.authenticate(jwt.issue(account.id(), Role.STUDENT, "sid"))).isEmpty();
    }

    @Test
    void garbageTokenIsRejectedWithoutTouchingRedis() {
        assertThat(authority.authenticate("rac")).isEmpty();
    }
}
