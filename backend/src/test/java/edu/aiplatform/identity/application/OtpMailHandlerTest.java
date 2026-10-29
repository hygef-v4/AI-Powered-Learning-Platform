package edu.aiplatform.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.aiplatform.identity.IdentityTestData;
import edu.aiplatform.identity.contract.AccountStatus;
import edu.aiplatform.identity.contract.IMail;
import edu.aiplatform.identity.contract.MailMessage;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.identity.domain.Account;
import edu.aiplatform.identity.domain.AccountRepository;
import edu.aiplatform.identity.domain.OtpPurpose;
import edu.aiplatform.identity.infrastructure.OtpChallengeStore;
import edu.aiplatform.jobs.contract.JobMessage;
import edu.aiplatform.jobs.contract.RetryableJobException;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OtpMailHandlerTest {

    private final AccountRepository accounts = mock(AccountRepository.class);
    private final OtpChallengeStore challenges = mock(OtpChallengeStore.class);
    private final IMail mail = mock(IMail.class);
    private final OtpMailHandler handler = new OtpMailHandler(accounts, challenges, mail, IdentityTestData.properties());

    private JobMessage message(Account account, OtpPurpose purpose) {
        return message(account, purpose, "k");
    }

    private JobMessage message(Account account, OtpPurpose purpose, String jobKey) {
        return new JobMessage(1, "OTP_DELIVERY", jobKey,
                Map.of("accountId", account.id().toString(), "purpose", purpose.name()), 0, "c");
    }

    @Test
    void storesCodeAndMailsTheSameCode() {
        Account pending = IdentityTestData.account(Role.STUDENT, AccountStatus.PENDING, null);
        when(accounts.findById(pending.id())).thenReturn(Optional.of(pending));
        when(challenges.issue(any(), any(), anyString(), anyString())).thenReturn(OtpChallengeStore.Issue.NEW);

        handler.handle(message(pending, OtpPurpose.ACTIVATION));

        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(challenges).issue(eq(pending.id()), eq(OtpPurpose.ACTIVATION), code.capture(), eq("k"));
        assertThat(code.getValue()).matches("\\d{6}");
        ArgumentCaptor<MailMessage> sent = ArgumentCaptor.forClass(MailMessage.class);
        verify(mail).send(sent.capture());
        assertThat(sent.getValue().to()).isEqualTo(pending.schoolEmail());
        assertThat(sent.getValue().textBody()).contains(code.getValue()).contains("10 phút");
    }

    @Test
    void accountNoLongerEligibleIsSkipped() {
        Account active = IdentityTestData.account(Role.STUDENT, AccountStatus.ACTIVE, "h");
        when(accounts.findById(active.id())).thenReturn(Optional.of(active));

        handler.handle(message(active, OtpPurpose.ACTIVATION));

        verify(challenges, never()).issue(any(), any(), anyString(), anyString());
        verify(mail, never()).send(any());
    }

    @Test
    void retryAfterMailFailureSendsTheSameCode() {
        Account pending = IdentityTestData.account(Role.STUDENT, AccountStatus.PENDING, null);
        when(accounts.findById(pending.id())).thenReturn(Optional.of(pending));
        when(challenges.issue(any(), any(), anyString(), anyString()))
                .thenReturn(OtpChallengeStore.Issue.NEW, OtpChallengeStore.Issue.RESEND);
        doThrow(new IllegalStateException("smtp down")).doNothing().when(mail).send(any());

        assertThatThrownBy(() -> handler.handle(message(pending, OtpPurpose.ACTIVATION, "job-7")))
                .isInstanceOf(RetryableJobException.class);
        handler.handle(message(pending, OtpPurpose.ACTIVATION, "job-7"));

        ArgumentCaptor<String> codes = ArgumentCaptor.forClass(String.class);
        verify(challenges, org.mockito.Mockito.times(2)).issue(eq(pending.id()), eq(OtpPurpose.ACTIVATION),
                codes.capture(), eq("job-7"));
        assertThat(codes.getAllValues().get(1)).as("lần chạy lại gửi đúng mã đã ghi").isEqualTo(codes.getAllValues().get(0));
    }

    @Test
    void staleRerunSendsNothing() {
        Account pending = IdentityTestData.account(Role.STUDENT, AccountStatus.PENDING, null);
        when(accounts.findById(pending.id())).thenReturn(Optional.of(pending));
        when(challenges.issue(any(), any(), anyString(), anyString())).thenReturn(OtpChallengeStore.Issue.STALE);

        handler.handle(message(pending, OtpPurpose.ACTIVATION, "job-8"));

        verify(mail, never()).send(any());
    }

    @Test
    void codeIsFixedPerJobAndDiffersAcrossJobs() {
        assertThat(handler.codeFor("job-1")).matches("\\d{6}").isEqualTo(handler.codeFor("job-1"));
        java.util.Set<String> codes = new java.util.HashSet<>();
        for (int i = 0; i < 50; i++) {
            codes.add(handler.codeFor("job-" + i));
        }
        assertThat(codes).as("mỗi job một mã").hasSizeGreaterThan(45);
    }

    @Test
    void smtpFailureIsRetryable() {
        Account active = IdentityTestData.account(Role.STUDENT, AccountStatus.ACTIVE, "h");
        when(accounts.findById(active.id())).thenReturn(Optional.of(active));
        when(challenges.issue(any(), any(), anyString(), anyString())).thenReturn(OtpChallengeStore.Issue.NEW);
        doThrow(new IllegalStateException("smtp down")).when(mail).send(any());

        assertThatThrownBy(() -> handler.handle(message(active, OtpPurpose.PASSWORD_RESET)))
                .isInstanceOf(RetryableJobException.class);
    }

    @Test
    void codesKeepLeadingZeros() {
        for (int i = 0; i < 200; i++) {
            assertThat(OtpMailHandler.newCode()).hasSize(6).containsOnlyDigits();
            assertThat(handler.codeFor("job-" + i)).hasSize(6).containsOnlyDigits();
        }
    }
}
