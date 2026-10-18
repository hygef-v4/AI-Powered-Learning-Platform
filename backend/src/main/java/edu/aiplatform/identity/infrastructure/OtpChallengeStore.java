package edu.aiplatform.identity.infrastructure;

import edu.aiplatform.identity.application.IdentityProperties;
import edu.aiplatform.identity.domain.OtpPurpose;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
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

    private final StringRedisTemplate redis;
    private final IdentityProperties properties;

    public OtpChallengeStore(StringRedisTemplate redis, IdentityProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    public enum Verification {
        VALID, INVALID
    }

    /** Ghi mã mới, xóa mã cũ cùng mục đích; mã rõ không được lưu. */
    public void replace(UUID accountId, OtpPurpose purpose, String code) {
        String key = key(accountId, purpose);
        redis.delete(key);
        redis.opsForHash().putAll(key, Map.of(CODE_HASH, hash(accountId, purpose, code),
                ATTEMPTS_LEFT, String.valueOf(properties.otpMaxAttempts())));
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

    /** Vô hiệu hóa tài khoản thì hủy mọi OTP còn hiệu lực (BR-U01-71). */
    public void deleteAll(UUID accountId) {
        for (OtpPurpose purpose : OtpPurpose.values()) {
            redis.delete(key(accountId, purpose));
        }
    }

    static String key(UUID accountId, OtpPurpose purpose) {
        return PREFIX + accountId + ":" + purpose.name();
    }

    static String hash(UUID accountId, OtpPurpose purpose, String code) {
        return Hashing.sha256Hex(accountId + ":" + purpose.name() + ":" + code.strip());
    }
}
