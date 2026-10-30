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
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

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
        when(sessions.create(any())).thenReturn(new SessionStore.IssuedSession("sid-1", "refresh-1"));
    }

    @Test
    void correctPasswordStartsSessionWithHomePathAndClearsThrottle() {
        AuthService.LoginResult result = service.login("  TaiVM@FPT.edu.vn ", PASSWORD, IP);

        assertThat(result.info().homePath()).isEqualTo(HomePaths.CLASS_DASHBOARD);
        assertThat(result.refreshToken()).isEqualTo("refresh-1");
        assertThat(jwt.parse(result.accessToken())).hasValueSatisfying(c -> assertThat(c.sid()).isEqualTo("sid-1"));
        verify(accounts).resetLoginThrottle(active.id());
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
        verify(sessions, never()).create(any());
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

    @Test
    void refreshIssuesAccessTokenWithCurrentRole() {
        Account promoted = new Account(active.id(), active.schoolEmail(), Role.TEACHER, AccountStatus.ACTIVE,
                active.passwordHash(), NOW, active.displayName(), null, 0, null, NOW, NOW, 4);
        when(sessions.rotate("r")).thenReturn(new SessionStore.Rotated("sid-1", active.id(), "r2"));
        when(accounts.findById(active.id())).thenReturn(Optional.of(promoted));

        AuthService.RefreshResult result = service.refresh("r");

        assertThat(result.refreshToken()).isEqualTo("r2");
        assertThat(jwt.parse(result.accessToken())).hasValueSatisfying(c -> assertThat(c.role()).isEqualTo(Role.TEACHER));
    }

    @Test
    void refreshForDisabledAccountRevokesSession() {
        Account disabled = IdentityTestData.account(Role.STUDENT, AccountStatus.DISABLED, "h");
        when(sessions.rotate("r")).thenReturn(new SessionStore.Rotated("sid-9", disabled.id(), "r2"));
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
        assertThat(HomePaths.of(Role.SUBJECT_MANAGER)).isEqualTo("/manager");
        assertThat(HomePaths.of(Role.ADMIN)).isEqualTo("/admin");
    }
}
