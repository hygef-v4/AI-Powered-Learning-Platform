package edu.aiplatform.identity.infrastructure;

import edu.aiplatform.identity.application.IdentityProperties;
import edu.aiplatform.identity.domain.OtpPurpose;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * {@code otpTicket} cấp sau khi OTP đúng (BR-U01-28): ngẫu nhiên 256 bit, Redis lưu băm {@code ticket:{hash}} → tài khoản
 * và mục đích, hết hạn 10 phút. Mật khẩu không đạt chính sách thì ticket giữ nguyên để nhập lại; đặt mật khẩu thành công
 * thì xóa (dùng một lần).
 */
@Component
public class VerificationTicketStore {

    static final String PREFIX = "ticket:";

    private final StringRedisTemplate redis;
    private final IdentityProperties properties;

    public VerificationTicketStore(StringRedisTemplate redis, IdentityProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    /** Trả ticket bản rõ cho client; Redis chỉ giữ băm. */
    public String issue(UUID accountId, OtpPurpose purpose) {
        String ticket = Hashing.randomToken(32);
        redis.opsForValue().set(PREFIX + Hashing.sha256Hex(ticket), purpose.name() + ":" + accountId,
                properties.otpTtl());
        return ticket;
    }

    /** Tài khoản của ticket nếu ticket còn hạn và đúng mục đích; không xóa. */
    public Optional<UUID> peek(String ticket, OtpPurpose purpose) {
        if (ticket == null || ticket.isBlank()) {
            return Optional.empty();
        }
        String value = redis.opsForValue().get(PREFIX + Hashing.sha256Hex(ticket));
        String prefix = purpose.name() + ":";
        if (value == null || !value.startsWith(prefix)) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(value.substring(prefix.length())));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public void consume(String ticket) {
        redis.delete(PREFIX + Hashing.sha256Hex(ticket));
    }
}
