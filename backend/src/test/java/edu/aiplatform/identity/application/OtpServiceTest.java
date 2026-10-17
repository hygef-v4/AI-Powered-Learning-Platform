package edu.aiplatform.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.aiplatform.identity.IdentityTestData;
import edu.aiplatform.identity.contract.AccountStatus;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.identity.domain.Account;
import edu.aiplatform.identity.domain.AccountRepository;
import edu.aiplatform.identity.domain.OtpPurpose;
import edu.aiplatform.identity.infrastructure.OtpChallengeStore;
import edu.aiplatform.identity.infrastructure.RequestThrottle;
import edu.aiplatform.identity.infrastructure.VerificationTicketStore;
import edu.aiplatform.jobs.contract.IJob;
import edu.aiplatform.jobs.contract.JobTypes;
import edu.aiplatform.shared.error.ApiException;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OtpServiceTest {

    private static final String IP = "10.0.0.1";

    private final AccountRepository accounts = mock(AccountRepository.class);
    private final RequestThrottle throttle = mock(RequestThrottle.class);
    private final OtpChallengeStore challenges = mock(OtpChallengeStore.class);
    private final VerificationTicketStore tickets = mock(VerificationTicketStore.class);
    private final IJob jobs = mock(IJob.class);
    private final Account pending = IdentityTestData.account(Role.STUDENT, AccountStatus.PENDING, null);
    private OtpService service;

    @BeforeEach
    void setUp() {
        service = new OtpService(accounts, IdentityTestData.properties(), throttle, challenges, tickets, jobs);
        when(throttle.tryOtpRequest(anyString(), anyString())).thenReturn(true);
        when(accounts.findByEmail("taivm@fpt.edu.vn")).thenReturn(Optional.of(pending));
    }

    @Test
    void eligibleRequestEnqueuesJobWithoutCode() {
        service.request(" TAIVM@fpt.edu.vn ", OtpPurpose.ACTIVATION, IP);

        verify(jobs).enqueue(eq(JobTypes.OTP_DELIVERY),
                eq(Map.of("accountId", pending.id().toString(), "purpose", "ACTIVATION")), anyString());
    }

    @Test
    void ineligibleRequestsAreSilent() {
        when(accounts.findByEmail("none@fpt.edu.vn")).thenReturn(Optional.empty());

        service.request("none@fpt.edu.vn", OtpPurpose.ACTIVATION, IP);
        service.request("taivm@gmail.com", OtpPurpose.ACTIVATION, IP);
        // Tài khoản PENDING không được đặt lại mật khẩu; ACTIVE không được kích hoạt lại.
        service.request("taivm@fpt.edu.vn", OtpPurpose.PASSWORD_RESET, IP);

        verify(jobs, never()).enqueue(anyString(), anyMap(), anyString());
    }

    @Test
    void rateLimitedRequestIsSilent() {
        when(throttle.tryOtpRequest(anyString(), anyString())).thenReturn(false);

        service.request("taivm@fpt.edu.vn", OtpPurpose.ACTIVATION, IP);

        verify(jobs, never()).enqueue(anyString(), anyMap(), anyString());
    }

    @Test
    void correctCodeReturnsTicket() {
        when(challenges.verify(pending.id(), OtpPurpose.ACTIVATION, "048213"))
                .thenReturn(OtpChallengeStore.Verification.VALID);
        when(tickets.issue(pending.id(), OtpPurpose.ACTIVATION)).thenReturn("ticket");

        assertThat(service.verify("taivm@fpt.edu.vn", OtpPurpose.ACTIVATION, "048213")).isEqualTo("ticket");
    }

    @Test
    void wrongCodeUnknownEmailAndBadFormatGiveSameError() {
        when(challenges.verify(any(), any(), anyString())).thenReturn(OtpChallengeStore.Verification.INVALID);
        when(accounts.findByEmail("none@fpt.edu.vn")).thenReturn(Optional.empty());

        for (String[] attempt : new String[][] {
                {"taivm@fpt.edu.vn", "111111"}, {"none@fpt.edu.vn", "048213"}, {"taivm@fpt.edu.vn", "12ab56"}}) {
            assertThatThrownBy(() -> service.verify(attempt[0], OtpPurpose.ACTIVATION, attempt[1]))
                    .isInstanceOfSatisfying(ApiException.class,
                            e -> assertThat(e.code()).isEqualTo(OtpService.OTP_INVALID));
        }
        verify(tickets, never()).issue(any(), any());
    }
}
