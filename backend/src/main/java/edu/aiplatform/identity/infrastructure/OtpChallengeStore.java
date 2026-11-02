package edu.aiplatform.identity.infrastructure;

import edu.aiplatform.identity.application.IdentityProperties;
import edu.aiplatform.identity.domain.OtpPurpose;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * OTP đang chờ trên Redis, khóa {@code otp:{accountId}:{purpose}} (infrastructure-design U01 mục 3): chỉ lưu băm và số
 * lượt còn lại, hết hạn sau 10 phút (BR-U01-20, 21). Mỗi tài khoản và mục đích một mã; mã mới thay mã cũ (BR-U01-23).
 * Sai đủ 5 lần thì xóa (BR-U01-22); đúng thì xóa ngay, dùng một lần (BR-U01-24). Băm gồm cả tài khoản và mục đích nên
 * mã của mục đích này không khớp mục đích kia (BR-U01-26).
 */
@Component
public class OtpChallengeStore {

    static final String PREFIX = "otp:";
    static final String CODE_HASH = "codeHash";
    static final String ATTEMPTS_LEFT = "attemptsLeft";
    /** Job gửi OTP đã ghi mã đang chờ; job chạy lại nhận ra mã của chính nó. */
    static final String JOB = "job";
    static final String JOB_PREFIX = "otp:job:";

    private final StringRedisTemplate redis;
    private final IdentityProperties properties;

    public OtpChallengeStore(StringRedisTemplate redis, IdentityProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    public enum Verification {
        VALID, INVALID
    }

    /** {@code NEW}: mã vừa ghi; {@code RESEND}: mã của job này vẫn đang chờ; {@code STALE}: đã dùng hoặc có mã mới hơn. */
    public enum Issue {
        NEW, RESEND, STALE
    }

    /** Ghi mã mới, xóa mã cũ cùng mục đích; mã rõ không được lưu. */
    public void replace(UUID accountId, OtpPurpose purpose, String code) {
        write(accountId, purpose, code, null);
    }

    /**
     * Ghi mã của job gửi OTP {@code jobKey} đúng một lần. Job chạy lại sau lỗi gửi mail gặp mã của chính nó thì gửi lại
     * đúng mã đó; mã đã dùng hoặc đã bị mã mới hơn thay thì bỏ qua, để lần chạy lại không hồi sinh mã cũ (BR-U01-23, 24).
     */
    public Issue issue(UUID accountId, OtpPurpose purpose, String code, String jobKey) {
        String marker = JOB_PREFIX + jobKey;
        if (Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(marker, "1", properties.otpTtl()))) {
            try {
                write(accountId, purpose, code, jobKey);
            } catch (RuntimeException e) {
                redis.delete(marker);
                throw e;
            }
            return Issue.NEW;
        }
        return jobKey.equals(redis.opsForHash().get(key(accountId, purpose), JOB)) ? Issue.RESEND : Issue.STALE;
    }

    private void write(UUID accountId, OtpPurpose purpose, String code, String jobKey) {
        String key = key(accountId, purpose);
        redis.delete(key);
        Map<String, String> fields = new HashMap<>(Map.of(CODE_HASH, hash(accountId, purpose, code),
                ATTEMPTS_LEFT, String.valueOf(properties.otpMaxAttempts())));
        if (jobKey != null) {
            fields.put(JOB, jobKey);
        }
        redis.opsForHash().putAll(key, fields);
        redis.expire(key, properties.otpTtl());
    }

    public Verification verify(UUID accountId, OtpPurpose purpose, String code) {
        String key = key(accountId, purpose);
        Object stored = redis.opsForHash().get(key, CODE_HASH);
        if (stored == null || code == null) {
            return Verification.INVALID;
        }
        boolean matches = MessageDigest.isEqual(stored.toString().getBytes(StandardCharsets.UTF_8),
                hash(accountId, purpose, code).getBytes(StandardCharsets.UTF_8));
        if (matches) {
            // Hai request đúng cùng lúc: chỉ request xóa được khóa mới thắng.
            return Boolean.TRUE.equals(redis.delete(key)) ? Verification.VALID : Verification.INVALID;
        }
        Long left = redis.opsForHash().increment(key, ATTEMPTS_LEFT, -1);
        if (left == null || left <= 0) {
            redis.delete(key);
        }
        return Verification.INVALID;
    }

    /** Đổi hoặc đặt lại mật khẩu xong thì hủy OTP đặt lại mật khẩu còn chờ. */
    public void delete(UUID accountId, OtpPurpose purpose) {
        redis.delete(key(accountId, purpose));
    }

    /** Vô hiệu hóa tài khoản thì hủy mọi OTP còn hiệu lực (BR-U01-71). */
    public void deleteAll(UUID accountId) {
        for (OtpPurpose purpose : OtpPurpose.values()) {
            delete(accountId, purpose);
        }
    }

    static String key(UUID accountId, OtpPurpose purpose) {
        return PREFIX + accountId + ":" + purpose.name();
    }

    static String hash(UUID accountId, OtpPurpose purpose, String code) {
        return Hashing.sha256Hex(accountId + ":" + purpose.name() + ":" + code.strip());
    }
}
