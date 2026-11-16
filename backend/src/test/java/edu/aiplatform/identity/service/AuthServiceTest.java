package edu.aiplatform.identity.service;

import static edu.aiplatform.identity.IdentityTestData.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.identity.IdentityTestData;
import edu.aiplatform.identity.contract.AccountStatus;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.identity.model.Account;
import edu.aiplatform.identity.repository.AccountRepository;
import edu.aiplatform.identity.integration.JwtService;
import edu.aiplatform.identity.integration.RequestThrottle;
import edu.aiplatform.identity.integration.SessionStore;
import edu.aiplatform.identity.integration.FakeRedis;
import edu.aiplatform.identity.integration.OtpChallengeStore;
import edu.aiplatform.identity.integration.VerificationTicketStore;
import edu.aiplatform.identity.model.OtpPurpose;
import com.fasterxml.jackson.databind.json.JsonMapper;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionOperations;

class AuthServiceTest {

    private static final String PASSWORD = "hocbai2026";
    private static final String IP = "10.0.0.1";

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final AccountRepository accounts = mock(AccountRepository.class);
    private final SessionStore sessions = mock(SessionStore.class);
    private final RequestThrottle throttle = mock(RequestThrottle.class);
    private final List<AuditEvent> audits = new ArrayList<>();
    private final IAudit audit = audits::add;
    private final JwtService jwt = new JwtService(IdentityTestData.properties(), Clock.fixed(NOW, ZoneOffset.UTC));
    private AuthService service;
    private Account active;

    @BeforeEach
    void setUp() {
        service = new AuthService(accounts, sessions, jwt, throttle, encoder, audit, IdentityTestData.properties(),
                Clock.fixed(NOW, ZoneOffset.UTC));
        active = IdentityTestData.account(Role.STUDENT, AccountStatus.ACTIVE, encoder.encode(PASSWORD));
        when(throttle.loginAllowed(anyString())).thenReturn(true);
        when(accounts.findByEmail("taivm@fpt.edu.vn")).thenReturn(Optional.of(active));
        when(sessions.create(any(), any())).thenReturn(new SessionStore.IssuedSession("sid-1", "refresh-1"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void loginPausedDuringPasswordResetCannotAuthenticateOrRefresh(boolean refresh) throws Exception {
        Clock afterReset = Clock.fixed(NOW.plusSeconds(1), ZoneOffset.UTC);
        SessionStore realSessions = new SessionStore(new FakeRedis().template,
                JsonMapper.builder().findAndAddModules().build(), afterReset, IdentityTestData.properties());
        CountDownLatch verifying = new CountDownLatch(1);
        CountDownLatch resume = new CountDownLatch(1);
        PasswordEncoder pausedEncoder = mock(PasswordEncoder.class);
        when(pausedEncoder.encode(any())).thenReturn("dummy");
        when(pausedEncoder.matches(PASSWORD, active.passwordHash())).thenAnswer(invocation -> {
            verifying.countDown();
            assertThat(resume.await(10, TimeUnit.SECONDS)).isTrue();
            return true;
        });
        AtomicReference<Account> current = new AtomicReference<>(active);
        when(accounts.findById(active.id())).thenAnswer(invocation -> Optional.of(current.get()));
        org.mockito.Mockito.doAnswer(invocation -> {
            current.set(new Account(active.id(), active.schoolEmail(), active.role(), active.status(),
                    invocation.getArgument(1), invocation.getArgument(2), active.displayName(),
                    active.phoneNumber(), 0, null, active.createdAt(), afterReset.instant(), active.version() + 1));
            return 1;
        }).when(accounts).updatePassword(eq(active.id()), anyString(), any());
        VerificationTicketStore tickets = mock(VerificationTicketStore.class);
        when(tickets.peek("reset", OtpPurpose.PASSWORD_RESET)).thenReturn(Optional.of(active.id()));
        when(tickets.take("reset", OtpPurpose.PASSWORD_RESET, active.id())).thenReturn(true);
        PasswordResetService reset = new PasswordResetService(tickets, accounts, realSessions,
                mock(OtpChallengeStore.class), encoder, audit, TransactionOperations.withoutTransaction(), afterReset);
        AuthService pausedAuth = new AuthService(accounts, realSessions, jwt, throttle, pausedEncoder,
                audit, IdentityTestData.properties(), afterReset);
        var pool = Executors.newSingleThreadExecutor();
        try {
            var login = pool.submit(() -> pausedAuth.login(active.schoolEmail(), PASSWORD, IP));
            assertThat(verifying.await(10, TimeUnit.SECONDS)).isTrue();
            reset.complete("reset", UUID.randomUUID().toString());
            resume.countDown();
            AuthService.LoginResult result = login.get(10, TimeUnit.SECONDS);
            String sid = jwt.parse(result.accessToken()).orElseThrow().sid();
            assertThat(realSessions.find(sid)).hasValueSatisfying(session ->
                    assertThat(session.pwdStamp()).isEqualTo(active.passwordChangedAt().toEpochMilli()));
            if (refresh) {
                assertThatThrownBy(() -> pausedAuth.refresh(result.refreshToken()))
                        .isInstanceOfSatisfying(ApiException.class, e -> {
                            assertThat(e.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
                            assertThat(e.code()).isEqualTo(AuthService.SESSION_EXPIRED);
                        });
            } else {
                SessionAuthority authority = new SessionAuthority(jwt, realSessions, accounts);
                assertThat(authority.authenticate(result.accessToken())).isEmpty();
            }
            assertThat(realSessions.find(sid)).isEmpty();
        } finally {
            resume.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    void correctPasswordStartsSessionWithHomePathAndClearsThrottle() {
        AuthService.LoginResult result = service.login("  TaiVM@FPT.edu.vn ", PASSWORD, IP);

        assertThat(result.info().homePath()).isEqualTo(HomePaths.CLASS_DASHBOARD);
        assertThat(result.refreshToken()).isEqualTo("refresh-1");
        assertThat(jwt.parse(result.accessToken())).hasValueSatisfying(c -> assertThat(c.sid()).isEqualTo("sid-1"));
        verify(accounts).resetLoginThrottle(active.id());
        verify(sessions).create(active.id(), active.passwordChangedAt().toEpochMilli());
    }

    @Test
    void everyFailureGivesTheSameError() {
        Account pending = IdentityTestData.account(Role.STUDENT, AccountStatus.PENDING, null);
        Account disabled = IdentityTestData.account(Role.STUDENT, AccountStatus.DISABLED, encoder.encode(PASSWORD));
        when(accounts.findByEmail("pending@fpt.edu.vn")).thenReturn(Optional.of(pending));
        when(accounts.findByEmail("disabled@fpt.edu.vn")).thenReturn(Optional.of(disabled));
        when(accounts.findByEmail("none@fpt.edu.vn")).thenReturn(Optional.empty());

        for (String[] attempt : new String[][] {
                {"taivm@fpt.edu.vn", "sai-mat-khau1"}, {"pending@fpt.edu.vn", PASSWORD},
                {"disabled@fpt.edu.vn", PASSWORD}, {"none@fpt.edu.vn", PASSWORD}}) {
            assertThatThrownBy(() -> service.login(attempt[0], attempt[1], IP))
                    .isInstanceOfSatisfying(ApiException.class, e -> {
                        assertThat(e.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
                        assertThat(e.code()).isEqualTo(AuthService.AUTH_FAILED);
                        assertThat(e.getMessage()).isEqualTo(AuthService.AUTH_FAILED_MESSAGE);
                    });
        }
        verify(sessions, never()).create(any(), any());
    }

    @Test
    void wrongPasswordIsCountedAndFifthFailureLocksAccount() {
        when(accounts.recordFailedLogin(eq(active.id()), eq(5), eq(NOW.plus(Duration.ofMinutes(15)))))
                .thenReturn(true);

        assertThatThrownBy(() -> service.login("taivm@fpt.edu.vn", "sai-mat-khau1", IP))
                .isInstanceOf(ApiException.class);

        assertThat(audits).extracting(AuditEvent::action).containsExactly("LOGIN_FAILED", "ACCOUNT_TEMP_LOCKED");
        assertThat(audits).allSatisfy(e -> assertThat(e.details()).isEmpty());
    }

    @Test
    void lockedAccountIsRefusedEvenWithCorrectPassword() {
        Account locked = IdentityTestData.locked(active, NOW.plusSeconds(60));
        when(accounts.findByEmail("taivm@fpt.edu.vn")).thenReturn(Optional.of(locked));

        assertThatThrownBy(() -> service.login("taivm@fpt.edu.vn", PASSWORD, IP))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo(AuthService.AUTH_FAILED));

        assertThat(audits).extracting(AuditEvent::action).containsExactly("LOGIN_BLOCKED_LOCKED");
        verify(accounts, never()).recordFailedLogin(any(), anyInt(), any());
    }

    @Test
    void expiredLockOpensByItself() {
        Account wasLocked = IdentityTestData.locked(active, NOW.minusSeconds(1));
        when(accounts.findByEmail("taivm@fpt.edu.vn")).thenReturn(Optional.of(wasLocked));

        assertThat(service.login("taivm@fpt.edu.vn", PASSWORD, IP).info().accountId()).isEqualTo(active.id());
    }

    @Test
    void clientRateLimitGivesNeutralErrorWithoutLookup() {
        when(throttle.loginAllowed(IP)).thenReturn(false);

        assertThatThrownBy(() -> service.login("taivm@fpt.edu.vn", PASSWORD, IP)).isInstanceOf(ApiException.class);
        verify(accounts, never()).findByEmail(anyString());
    }

    @Test
    void onlyFailedLoginsSpendClientAttempts() {
        service.login("taivm@fpt.edu.vn", PASSWORD, IP);
        verify(throttle, never()).recordLoginFailure(anyString());

        assertThatThrownBy(() -> service.login("taivm@fpt.edu.vn", "sai-mat-khau1", IP))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.login("khong-co@fpt.edu.vn", PASSWORD, IP))
                .isInstanceOf(ApiException.class);
        verify(throttle, org.mockito.Mockito.times(2)).recordLoginFailure(IP);
    }

    @Test
    void passwordLongerThan72BytesNeverMatches() {
        assertThatThrownBy(() -> service.login("taivm@fpt.edu.vn", PASSWORD + "x".repeat(80), IP))
                .isInstanceOf(ApiException.class);
    }

    @ParameterizedTest
    @CsvSource(value = {"-1,1,false", "0,-1,true", "1,-1,true", "legacy,-1,false",
            "legacy,0,true", "legacy,1,true"}, nullValues = "legacy")
    void refreshChecksStampAndLegacyIssueTime(Long stampOffset, long issueOffset, boolean accepted) {
        var session = new SessionStore.SessionRecord(active.id(), NOW.plusMillis(issueOffset), NOW,
                "hash", stampOffset == null ? null : NOW.toEpochMilli() + stampOffset);
        when(sessions.rotate("r")).thenReturn(new SessionStore.Rotated("sid-1", session, "r2"));
        when(accounts.findById(active.id())).thenReturn(Optional.of(active));

        if (accepted) {
            assertThat(service.refresh("r").refreshToken()).isEqualTo("r2");
            verify(sessions, never()).delete(any());
        } else {
            assertThatThrownBy(() -> service.refresh("r"))
                    .isInstanceOfSatisfying(ApiException.class, e -> {
                        assertThat(e.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
                        assertThat(e.code()).isEqualTo(AuthService.SESSION_EXPIRED);
                    });
            verify(sessions).delete("sid-1");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"foreign", "rotated", "stale", "deleted"})
    void staleRefreshSnapshotCannotAcceptAnInvalidReplacement(String replacement) {
        var oldSession = new SessionStore.SessionRecord(active.id(), NOW, NOW,
                "hash", NOW.minusMillis(1).toEpochMilli());
        var liveSession = new SessionStore.SessionRecord(
                replacement.equals("foreign") ? UUID.randomUUID() : active.id(), NOW, NOW,
                replacement.equals("rotated") ? "other-hash" : "hash",
                replacement.equals("stale") ? NOW.minusMillis(1).toEpochMilli() : NOW.toEpochMilli());
        when(sessions.rotate("r")).thenReturn(new SessionStore.Rotated("sid-1", oldSession, "r2"));
        when(accounts.findById(active.id())).thenReturn(Optional.of(active));
        when(sessions.find("sid-1")).thenReturn(replacement.equals("deleted")
                ? Optional.empty() : Optional.of(liveSession));

        assertThatThrownBy(() -> service.refresh("r"))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(e.code()).isEqualTo(AuthService.SESSION_EXPIRED);
                });
        verify(sessions).find("sid-1");
        verify(sessions).delete("sid-1");
    }

    @Test
    void refreshSurvivesRestampAfterRotation() {
        var realSessions = new SessionStore(new FakeRedis().template,
                JsonMapper.builder().findAndAddModules().build(), Clock.fixed(NOW, ZoneOffset.UTC),
                IdentityTestData.properties());
        var issued = realSessions.create(active.id(), NOW.minusMillis(1).toEpochMilli());
        when(accounts.findById(active.id())).thenAnswer(invocation -> {
            realSessions.restamp(issued.sid(), NOW.toEpochMilli());
            return Optional.of(active);
        });
        var authService = new AuthService(accounts, realSessions, jwt, throttle, encoder, audit,
                IdentityTestData.properties(), Clock.fixed(NOW, ZoneOffset.UTC));

        var refreshed = authService.refresh(issued.refreshToken());

        assertThat(realSessions.sidOf(refreshed.refreshToken())).hasValue(issued.sid());
        assertThat(realSessions.find(issued.sid())).hasValueSatisfying(session ->
                assertThat(session.isPasswordCurrent(active.passwordChangedAt())).isTrue());
    }

    @Test
    void refreshIssuesAccessTokenWithCurrentRole() {
        Account promoted = new Account(active.id(), active.schoolEmail(), Role.TEACHER, AccountStatus.ACTIVE,
                active.passwordHash(), NOW, active.displayName(), null, 0, null, NOW, NOW, 4);
        when(sessions.rotate("r")).thenReturn(new SessionStore.Rotated("sid-1",
                new SessionStore.SessionRecord(active.id(), NOW, NOW, "hash", NOW.toEpochMilli()), "r2"));
        when(accounts.findById(active.id())).thenReturn(Optional.of(promoted));

        AuthService.RefreshResult result = service.refresh("r");

        assertThat(result.refreshToken()).isEqualTo("r2");
        assertThat(jwt.parse(result.accessToken())).hasValueSatisfying(c -> assertThat(c.role()).isEqualTo(Role.TEACHER));
        verify(sessions, never()).find(any());
    }

    @Test
    void refreshForDisabledAccountRevokesSession() {
        Account disabled = IdentityTestData.account(Role.STUDENT, AccountStatus.DISABLED, "h");
        when(sessions.rotate("r")).thenReturn(new SessionStore.Rotated("sid-9",
                new SessionStore.SessionRecord(disabled.id(), NOW, NOW, "hash", NOW.toEpochMilli()), "r2"));
        when(accounts.findById(disabled.id())).thenReturn(Optional.of(disabled));

        assertThatThrownBy(() -> service.refresh("r"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo(AuthService.SESSION_EXPIRED));
        verify(sessions).delete("sid-9");
    }

    @Test
    void rejectedRefreshIsUnauthorized() {
        when(sessions.rotate("old")).thenReturn(new SessionStore.Rejected(SessionStore.Reason.REUSED));

        assertThatThrownBy(() -> service.refresh("old"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.status()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void concurrentRefreshIsConflictWithoutRevokingSession() {
        when(sessions.rotate("r")).thenReturn(new SessionStore.Rejected(SessionStore.Reason.CONCURRENT));

        assertThatThrownBy(() -> service.refresh("r"))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.code()).isEqualTo(AuthService.REFRESH_CONCURRENT);
                });
        verify(sessions, never()).delete(any());
    }

    @Test
    void logoutDeletesOnlyCurrentSession() {
        service.logout("sid-1", null);
        verify(sessions).delete("sid-1");
        service.logout(null, null);
        verify(sessions, never()).deleteAll(any(UUID.class));
    }

    @Test
    void logoutWithExpiredAccessTokenRevokesSessionOfRefreshToken() {
        when(sessions.sidOf("r")).thenReturn(Optional.of("sid-2"));

        service.logout(null, "r");

        verify(sessions).delete("sid-2");
    }

    @Test
    void logoutDoesNotDeleteSameSessionTwice() {
        when(sessions.sidOf("r")).thenReturn(Optional.of("sid-1"));

        service.logout("sid-1", "r");

        verify(sessions, times(1)).delete("sid-1");
    }

    @Test
    void homePathFollowsRole() {
        assertThat(HomePaths.of(Role.STUDENT)).isEqualTo("/classes");
        assertThat(HomePaths.of(Role.TEACHER)).isEqualTo("/classes");
        assertThat(HomePaths.of(Role.SUBJECT_MANAGER)).isEqualTo("/teaching/classes");
        assertThat(HomePaths.of(Role.ADMIN)).isEqualTo("/admin");
    }
}
