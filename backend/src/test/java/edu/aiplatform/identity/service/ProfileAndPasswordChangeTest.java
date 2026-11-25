package edu.aiplatform.identity.service;

import static edu.aiplatform.identity.IdentityTestData.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.AuthorizationDecision;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.identity.model.Account;
import edu.aiplatform.identity.repository.AccountRepository;
import edu.aiplatform.identity.model.OtpPurpose;
import edu.aiplatform.identity.integration.OtpChallengeStore;
import edu.aiplatform.identity.integration.RequestThrottle;
import edu.aiplatform.identity.integration.SessionStore;
import edu.aiplatform.identity.integration.FakeRedis;
import edu.aiplatform.identity.integration.JwtService;
import com.fasterxml.jackson.databind.json.JsonMapper;
import edu.aiplatform.identity.integration.VerificationTicketStore;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.support.TransactionOperations;

/** UC 05 đổi mật khẩu, UC 06/07 hồ sơ của chính mình. */
class ProfileAndPasswordChangeTest {

    private static final String PASSWORD = "hocbai2026";
    // Sinh mỗi lần chạy để không có chuỗi mật khẩu mới cố định trong mã.
    private static final String NEW_PASSWORD = UUID.randomUUID().toString();

    private final AccountRepository accounts = mock(AccountRepository.class);
    private final IAuthorization authorization = mock(IAuthorization.class);
    private final SessionStore sessions = mock(SessionStore.class);
    private final OtpChallengeStore otpChallenges = mock(OtpChallengeStore.class);
    private final VerificationTicketStore tickets = mock(VerificationTicketStore.class);
    private final RequestThrottle throttle = mock(RequestThrottle.class);
    private final List<AuditEvent> audits = new ArrayList<>();
    private final IAudit audit = audits::add;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final ProfileService profiles = new ProfileService(accounts, authorization, audit,
            TransactionOperations.withoutTransaction(), clock);
    private final PasswordChangeService passwords = new PasswordChangeService(accounts, authorization, sessions,
            otpChallenges, tickets, throttle, encoder, audit, TransactionOperations.withoutTransaction(), clock);
    private Account account;
    private Actor actor;

    @BeforeEach
    void setUp() {
        account = IdentityTestData.account(Role.STUDENT, AccountStatus.ACTIVE, encoder.encode(PASSWORD));
        actor = new Actor(account.id(), Role.STUDENT);
        when(accounts.findById(account.id())).thenReturn(Optional.of(account));
        when(authorization.authorize(any(), anyString(), any())).thenReturn(AuthorizationDecision.allow());
        when(throttle.isPasswordChangeAllowed(account.id())).thenReturn(true);
    }

    @Test
    void profileShowsOwnDataWithoutSecrets() {
        ProfileService.ProfileView view = profiles.loadProfile(actor);
        assertThat(view.email()).isEqualTo("taivm@fpt.edu.vn");
        assertThat(view.homePath()).isEqualTo(HomePaths.CLASS_DASHBOARD);
    }

    @Test
    void updateTrimsNameAndAuditsFieldNamesOnly() {
        profiles.updateProfile(actor, "  Vũ Minh Tài Mới ", " 0912345678 ");

        verify(accounts).updateProfile(account.id(), "Vũ Minh Tài Mới", "0912345678", null, NOW);
        assertThat(audits).singleElement().satisfies(e -> {
            assertThat(e.action()).isEqualTo("PROFILE_UPDATED");
            assertThat(e.details()).isEqualTo(Map.of("fields", List.of("displayName", "phoneNumber")));
            assertThat(e.details().toString()).doesNotContain("0912345678");
        });
    }

    @Test
    void invalidFieldsAreRejected() {
        assertThatThrownBy(() -> profiles.updateProfile(actor, "   ", null))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.readCode()).isEqualTo("INVALID_DISPLAY_NAME"));
        assertThatThrownBy(() -> profiles.updateProfile(actor, "Tài", "abc"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.readCode()).isEqualTo("INVALID_PHONE_NUMBER"));
    }

    @Test
    void deniedActorIsForbidden() {
        when(authorization.authorize(any(), anyString(), any())).thenReturn(AuthorizationDecision.deny("X"));
        assertThatThrownBy(() -> profiles.loadProfile(actor))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.readStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void changedPasswordRestampsCurrentSessionAndRejectsLateOldPasswordLogin() {
        Clock changeClock = Clock.fixed(NOW.plusSeconds(1).plusNanos(123456), ZoneOffset.UTC);
        SessionStore realSessions = new SessionStore(new FakeRedis().template,
                JsonMapper.builder().findAndAddModules().build(), changeClock, IdentityTestData.properties());
        var keep = realSessions.createSession(account.id(), NOW.toEpochMilli());
        var other = realSessions.createSession(account.id(), NOW.toEpochMilli());
        PasswordChangeService changeService = new PasswordChangeService(accounts, authorization, realSessions,
                otpChallenges, tickets, throttle, encoder, audit, TransactionOperations.withoutTransaction(), changeClock);

        changeService.change(actor, keep.sid(), PASSWORD, NEW_PASSWORD);

        var changedAt = NOW.plusSeconds(1);
        verify(accounts).updatePassword(eq(account.id()), anyString(), eq(changedAt));
        assertThat(realSessions.findSession(keep.sid())).hasValueSatisfying(session -> {
            assertThat(session.pwdStamp()).isEqualTo(changedAt.toEpochMilli());
            assertThat(session.isPasswordCurrent(account.passwordChangedAt())).isTrue();
        });
        Account changed = new Account(account.id(), account.schoolEmail(), account.role(), account.status(),
                encoder.encode(NEW_PASSWORD), changedAt, account.displayName(), account.phoneNumber(), 0, null,
                account.createdAt(), changedAt, account.version() + 1);
        when(accounts.findById(account.id())).thenReturn(Optional.of(changed));
        JwtService jwt = new JwtService(IdentityTestData.properties(), changeClock);
        SessionAuthority authority = new SessionAuthority(jwt, realSessions, accounts);
        assertThat(authority.authenticate(jwt.issue(account.id(), account.role(), keep.sid()))).isPresent();
        assertThat(realSessions.findSession(other.sid())).isEmpty();
        var lateLogin = realSessions.createSession(account.id(), NOW.toEpochMilli());
        assertThat(authority.authenticate(jwt.issue(account.id(), account.role(), lateLogin.sid()))).isEmpty();
        assertThat(realSessions.findSession(lateLogin.sid())).isEmpty();
    }

    @Test
    void restampFailureAbortsPasswordChangeInsteadOfReportingSuccess() {
        org.mockito.Mockito.doThrow(new IllegalStateException("CAS contention"))
                .when(sessions).restampSession("sid-now", NOW.toEpochMilli());

        assertThatThrownBy(() -> passwords.change(actor, "sid-now", PASSWORD, NEW_PASSWORD))
                .isInstanceOf(IllegalStateException.class);

        verify(sessions, never()).deleteAllExcept(any(), any());
        verify(otpChallenges, never()).deleteChallenge(any(), any());
        verify(tickets, never()).revoke(any(), any());
    }

    @Test
    void changePasswordKeepsCurrentSessionAndRevokesOthers() {
        passwords.change(actor, "sid-now", PASSWORD, NEW_PASSWORD);

        verify(accounts).updatePassword(eq(account.id()), org.mockito.ArgumentMatchers.argThat(
                hash -> encoder.matches(NEW_PASSWORD, hash)), eq(NOW));
        var order = org.mockito.Mockito.inOrder(sessions);
        order.verify(sessions).restampSession("sid-now", NOW.toEpochMilli());
        order.verify(sessions, times(2)).deleteAllExcept(account.id(), "sid-now");
        verify(sessions, never()).deleteAll(any());
        verify(otpChallenges).deleteChallenge(account.id(), OtpPurpose.PASSWORD_RESET);
        verify(tickets).revoke(account.id(), OtpPurpose.PASSWORD_RESET);
        verify(throttle, never()).recordPasswordChangeFailure(any());
        assertThat(audits).extracting(AuditEvent::action).containsExactly("PASSWORD_CHANGED");
    }

    @Test
    void tooManyWrongCurrentPasswordsAreThrottledBeforeBcrypt() {
        when(throttle.isPasswordChangeAllowed(account.id())).thenReturn(false);

        assertThatThrownBy(() -> passwords.change(actor, "sid", PASSWORD, NEW_PASSWORD))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.readStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                    assertThat(e.readCode()).isEqualTo(PasswordChangeService.TOO_MANY_ATTEMPTS);
                });
        verify(accounts, never()).updatePassword(any(), anyString(), any());
    }

    @Test
    void dependencyFailureIsSystemBusy() {
        when(authorization.authorize(any(), anyString(), any()))
                .thenReturn(AuthorizationDecision.deny(AuthorizationService.DEPENDENCY_UNAVAILABLE));
        assertThatThrownBy(() -> profiles.loadProfile(actor)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.readStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
        assertThatThrownBy(() -> passwords.change(actor, "sid", PASSWORD, NEW_PASSWORD))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.readStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    void weakNewPasswordIsRejected() {
        assertThatThrownBy(() -> passwords.change(actor, "sid", PASSWORD, "taivm123"))
                .isInstanceOf(PasswordPolicyException.class);
        verify(accounts, never()).updatePassword(any(), anyString(), any());
    }
}
