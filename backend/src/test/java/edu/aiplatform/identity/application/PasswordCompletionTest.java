package edu.aiplatform.identity.application;

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
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.identity.domain.Account;
import edu.aiplatform.identity.domain.AccountRepository;
import edu.aiplatform.identity.domain.OtpPurpose;
import edu.aiplatform.identity.domain.PasswordPolicy;
import edu.aiplatform.identity.infrastructure.OtpChallengeStore;
import edu.aiplatform.identity.infrastructure.SessionStore;
import edu.aiplatform.identity.infrastructure.VerificationTicketStore;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.support.TransactionOperations;

/** Bước 3 của kích hoạt (F2) và đặt lại mật khẩu (F5). */
class PasswordCompletionTest {

    private final VerificationTicketStore tickets = mock(VerificationTicketStore.class);
    private final AccountRepository accounts = mock(AccountRepository.class);
    private final SessionStore sessions = mock(SessionStore.class);
    private final OtpChallengeStore otpChallenges = mock(OtpChallengeStore.class);
    private final AuthService auth = mock(AuthService.class);
    private final List<AuditEvent> audits = new ArrayList<>();
    private final IAudit audit = audits::add;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private final ActivationService activation = new ActivationService(tickets, accounts, encoder, auth, audit,
            TransactionOperations.withoutTransaction(), clock);
    private final PasswordResetService reset = new PasswordResetService(tickets, accounts, sessions, otpChallenges,
            encoder, audit,
            TransactionOperations.withoutTransaction(), clock);

    @Test
    void activationStoresPasswordAuditsAndSignsIn() {
        Account pending = IdentityTestData.account(Role.TEACHER, AccountStatus.PENDING, null);
        Account active = IdentityTestData.account(Role.TEACHER, AccountStatus.ACTIVE, "h");
        when(tickets.peek("t", OtpPurpose.ACTIVATION)).thenReturn(Optional.of(pending.id()));
        when(accounts.findById(pending.id())).thenReturn(Optional.of(pending), Optional.of(active));
        when(accounts.activate(eq(pending.id()), anyString(), eq(NOW))).thenReturn(1);
        when(tickets.take("t", OtpPurpose.ACTIVATION, pending.id())).thenReturn(true);
        AuthService.LoginResult session = new AuthService.LoginResult(
                new SessionInfo(pending.id(), "Tài", Role.TEACHER, "/classes"), "a", "r");
        when(auth.startSession(active)).thenReturn(session);

        assertThat(activation.complete("t", "hocbai2026")).isSameAs(session);

        verify(accounts).activate(eq(pending.id()), org.mockito.ArgumentMatchers.argThat(
                hash -> encoder.matches("hocbai2026", hash)), eq(NOW));
        verify(tickets).take("t", OtpPurpose.ACTIVATION, pending.id());
        assertThat(audits).extracting(AuditEvent::action).containsExactly("ACCOUNT_ACTIVATED");
    }

    @Test
    void weakPasswordKeepsTicketForRetry() {
        Account pending = IdentityTestData.account(Role.STUDENT, AccountStatus.PENDING, null);
        when(tickets.peek("t", OtpPurpose.ACTIVATION)).thenReturn(Optional.of(pending.id()));
        when(accounts.findById(pending.id())).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> activation.complete("t", "abc"))
                .isInstanceOfSatisfying(PasswordPolicyException.class,
                        e -> assertThat(e.violations()).contains(PasswordPolicy.Violation.TOO_SHORT));
        verify(tickets, never()).take(anyString(), any(), any());
        verify(accounts, never()).activate(any(), anyString(), any());
    }

    @Test
    void ticketAlreadyTakenByConcurrentRequestIsRejected() {
        Account active = IdentityTestData.account(Role.STUDENT, AccountStatus.ACTIVE, "h");
        when(tickets.peek("t", OtpPurpose.PASSWORD_RESET)).thenReturn(Optional.of(active.id()));
        when(accounts.findById(active.id())).thenReturn(Optional.of(active));
        when(tickets.take("t", OtpPurpose.PASSWORD_RESET, active.id())).thenReturn(false);

        assertThatThrownBy(() -> reset.complete("t", "matkhaumoi9"))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo(ActivationService.TICKET_EXPIRED));
        verify(accounts, never()).updatePassword(any(), anyString(), any());
        assertThat(audits).isEmpty();
    }

    @Test
    void expiredOrWrongPurposeTicketIsRejected() {
        when(tickets.peek(anyString(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> activation.complete("t", "hocbai2026"))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo(ActivationService.TICKET_EXPIRED));
        assertThatThrownBy(() -> reset.complete("t", "hocbai2026")).isInstanceOf(ApiException.class);
    }

    @Test
    void alreadyActivatedAccountCannotUseActivationTicket() {
        Account active = IdentityTestData.account(Role.STUDENT, AccountStatus.ACTIVE, "h");
        when(tickets.peek("t", OtpPurpose.ACTIVATION)).thenReturn(Optional.of(active.id()));
        when(accounts.findById(active.id())).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> activation.complete("t", "hocbai2026")).isInstanceOf(ApiException.class);
    }

    @Test
    void resetSavesPasswordAndRevokesEverySessionWithoutSigningIn() {
        Account active = IdentityTestData.account(Role.STUDENT, AccountStatus.ACTIVE, "h");
        when(tickets.peek("t", OtpPurpose.PASSWORD_RESET)).thenReturn(Optional.of(active.id()));
        when(accounts.findById(active.id())).thenReturn(Optional.of(active));
        when(tickets.take("t", OtpPurpose.PASSWORD_RESET, active.id())).thenReturn(true);

        reset.complete("t", "matkhaumoi9");

        verify(accounts).updatePassword(eq(active.id()), anyString(), eq(NOW));
        verify(sessions, times(2)).deleteAll(active.id());
        verify(otpChallenges).delete(active.id(), OtpPurpose.PASSWORD_RESET);
        verify(auth, never()).startSession(any());
        assertThat(audits).extracting(AuditEvent::action).containsExactly("PASSWORD_RESET");
    }
}
