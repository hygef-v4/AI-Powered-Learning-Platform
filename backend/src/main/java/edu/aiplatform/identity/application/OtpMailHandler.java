package edu.aiplatform.identity.application;

import edu.aiplatform.identity.contract.IMail;
import edu.aiplatform.identity.domain.Account;
import edu.aiplatform.identity.domain.AccountRepository;
import edu.aiplatform.identity.domain.OtpPurpose;
import edu.aiplatform.identity.infrastructure.OtpChallengeStore;
import edu.aiplatform.jobs.contract.IJobHandler;
import edu.aiplatform.jobs.contract.JobMessage;
import edu.aiplatform.jobs.contract.JobQueue;
import edu.aiplatform.jobs.contract.JobTypes;
import edu.aiplatform.jobs.contract.RetryableJobException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Worker gửi OTP ({@code OTP_DELIVERY}, P9): sinh mã 6 số ngay trong worker, lưu băm vào Redis (xóa mã cũ) rồi gửi mail.
 * Mã rõ chỉ có trong bộ nhớ worker và trong email, không qua queue, database hay log (BR-U01-27). Redis hoặc SMTP lỗi thì
 * U03 thử lại (mỗi lần sinh mã mới); hết lượt thì log {@code OTP_DELIVERY_FAILED}, người dùng yêu cầu lại (BR-U01-92).
 */
@Component
public class OtpMailHandler implements IJobHandler {

    private static final Logger log = LoggerFactory.getLogger(OtpMailHandler.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccountRepository accounts;
    private final OtpChallengeStore challenges;
    private final IMail mail;
    private final IdentityProperties properties;

    public OtpMailHandler(AccountRepository accounts, OtpChallengeStore challenges, IMail mail,
                          IdentityProperties properties) {
        this.accounts = accounts;
        this.challenges = challenges;
        this.mail = mail;
        this.properties = properties;
    }

    @Override
    public String jobType() {
        return JobTypes.OTP_DELIVERY;
    }

    @Override
    public JobQueue queue() {
        return JobQueue.EMAIL;
    }

    @Override
    public Duration timeout() {
        return Duration.ofSeconds(30);
    }

    @Override
    public void handle(JobMessage message) {
        UUID accountId = UUID.fromString(String.valueOf(message.payload().get("accountId")));
        OtpPurpose purpose = OtpPurpose.valueOf(String.valueOf(message.payload().get("purpose")));
        // Trạng thái có thể đã đổi từ lúc yêu cầu (ví dụ đã kích hoạt, bị vô hiệu hóa): khi đó bỏ qua.
        Optional<Account> account = accounts.findById(accountId).filter(purpose::appliesTo);
        if (account.isEmpty()) {
            log.info("Bỏ qua OTP_DELIVERY {}: tài khoản không còn phù hợp", purpose);
            return;
        }
        String code = codeFor(message.idempotencyKey());
        try {
            if (challenges.issue(accountId, purpose, code, message.idempotencyKey()) == OtpChallengeStore.Issue.STALE) {
                log.info("Bỏ qua OTP_DELIVERY {}: mã của lần gửi này đã dùng hoặc đã có mã mới hơn", purpose);
                return;
            }
            mail.send(OtpMailTemplate.build(account.get().schoolEmail(), purpose, code, properties.otpTtl()));
        } catch (RuntimeException e) {
            throw new RetryableJobException("Gửi OTP lỗi: " + e.getClass().getSimpleName(), e);
        }
    }

    @Override
    public void onFailed(JobMessage message, String error) {
        log.error("OTP_DELIVERY_FAILED key={} lỗi={}", message.idempotencyKey(), error);
    }

    /**
     * Mã 6 chữ số của một lần gửi, suy ra cố định từ khóa job bằng HMAC với khóa ký của U01 (nhãn riêng, không dùng chung
     * với chữ ký JWT): job chạy lại sau lỗi gửi mail gửi đúng mã đã ghi, không ra email thứ hai với mã khác. Khóa job là
     * UUID ngẫu nhiên do server tạo và không lộ ra ngoài; thiếu khóa thì dùng mã ngẫu nhiên.
     */
    String codeFor(String jobKey) {
        if (jobKey == null || jobKey.isBlank()) {
            return newCode();
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.jwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(("otp-code:" + jobKey).getBytes(StandardCharsets.UTF_8));
            long value = ByteBuffer.wrap(digest, 0, 8).getLong() & Long.MAX_VALUE;
            return String.format("%06d", value % 1_000_000);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 không khả dụng", e);
        }
    }

    /** 6 chữ số, giữ số 0 ở đầu (BR-U01-20). */
    static String newCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }
}
