package edu.aiplatform.identity.service;

import edu.aiplatform.identity.config.IdentityProperties;

import edu.aiplatform.identity.model.Account;
import edu.aiplatform.identity.repository.AccountRepository;
import edu.aiplatform.identity.model.EmailPolicy;
import edu.aiplatform.identity.model.OtpPurpose;
import edu.aiplatform.identity.integration.OtpChallengeStore;
import edu.aiplatform.identity.integration.RequestThrottle;
import edu.aiplatform.identity.integration.VerificationTicketStore;
import edu.aiplatform.jobs.contract.IJob;
import edu.aiplatform.jobs.contract.JobTypes;
import edu.aiplatform.shared.error.ApiException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Yêu cầu và xác minh OTP cho kích hoạt (F1, F2 bước 2a) và đặt lại mật khẩu (F5 bước 1–4).
 * Yêu cầu luôn được trả lời như nhau, không lộ tài khoản có tồn tại hay trạng thái (BR-U01-12, 25). Request không sinh
 * mã: chỉ gửi việc {@code OTP_DELIVERY} chứa ID và mục đích; worker sinh mã và gửi mail (P9).
 */
@Service
public class OtpService {

    public static final String OTP_INVALID = "OTP_INVALID";
    public static final String OTP_INVALID_MESSAGE = "Mã không hợp lệ hoặc đã hết hạn.";

    private final AccountRepository accounts;
    private final EmailPolicy emailPolicy;
    private final RequestThrottle throttle;
    private final OtpChallengeStore challenges;
    private final VerificationTicketStore tickets;
    private final IJob jobs;

    public OtpService(AccountRepository accounts, IdentityProperties properties, RequestThrottle throttle,
                      OtpChallengeStore challenges, VerificationTicketStore tickets, IJob jobs) {
        this.accounts = accounts;
        this.emailPolicy = new EmailPolicy(properties.allowedEmailDomains());
        this.throttle = throttle;
        this.challenges = challenges;
        this.tickets = tickets;
        this.jobs = jobs;
    }

    /** Chỉ gửi việc khi email hợp lệ, trong hạn mức, khớp tài khoản đúng trạng thái; mọi trường hợp đều im lặng. */
    public void request(String rawEmail, OtpPurpose purpose, String clientIp) {
        String email = EmailPolicy.normalize(rawEmail);
        if (emailPolicy.check(email) != EmailPolicy.EmailCheck.OK || !throttle.tryOtpRequest(email, clientIp)) {
            return;
        }
        Optional<Account> account = accounts.findByEmail(email).filter(purpose::appliesTo);
        account.ifPresent(a -> jobs.enqueue(JobTypes.OTP_DELIVERY,
                Map.of("accountId", a.id().toString(), "purpose", purpose.name()), UUID.randomUUID().toString()));
    }

    /** OTP đúng thì trả {@code otpTicket} để sang bước đặt mật khẩu (BR-U01-28); sai thì một lỗi chung. */
    public String verify(String rawEmail, OtpPurpose purpose, String code) {
        String email = EmailPolicy.normalize(rawEmail);
        Optional<Account> account = accounts.findByEmail(email).filter(purpose::appliesTo);
        if (account.isEmpty() || code == null || !code.strip().matches("\\d{6}")
                || challenges.verify(account.get().id(), purpose, code) != OtpChallengeStore.Verification.VALID) {
            throw ApiException.badRequest(OTP_INVALID, OTP_INVALID_MESSAGE);
        }
        return tickets.issue(account.get().id(), purpose);
    }
}
