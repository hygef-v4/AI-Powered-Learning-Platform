package edu.aiplatform.identity.integration;

import edu.aiplatform.identity.config.IdentityProperties;
import edu.aiplatform.identity.model.OtpPurpose;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * {@code otpTicket} cấp sau khi OTP đúng (BR-U01-28): ngẫu nhiên 256 bit, Redis lưu băm {@code ticket:{hash}} → tài khoản
 * và mục đích, hết hạn 10 phút. Mỗi tài khoản và mục đích chỉ giữ một ticket ({@code ticket:account:{id}:{purpose}} trỏ
 * tới băm hiện hành): ticket mới thay ticket cũ, và đổi/đặt lại mật khẩu hay vô hiệu hóa thu hồi được ticket đang chờ.
 * Mật khẩu không đạt chính sách thì ticket giữ nguyên để nhập lại; đặt mật khẩu thì {@link #take} lấy và xóa nguyên tử
 * nên hai request cùng ticket chỉ một cái dùng được.
 */
@Component
public class VerificationTicketStore {

    static final String PREFIX = "ticket:";
    static final String ACCOUNT_PREFIX = "ticket:account:";

    private final StringRedisTemplate redis;
    private final IdentityProperties properties;

    public VerificationTicketStore(StringRedisTemplate redis, IdentityProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    /** Trả ticket bản rõ cho client; Redis chỉ giữ băm. Ticket cũ cùng tài khoản và mục đích bị xóa. */
    public String issue(UUID accountId, OtpPurpose purpose) {
        String ticket = Hashing.randomToken(32);
        String hash = Hashing.sha256Hex(ticket);
        String index = indexKey(accountId, purpose);
        String previous = redis.opsForValue().get(index);
        if (previous != null) {
            redis.delete(PREFIX + previous);
        }
        redis.opsForValue().set(PREFIX + hash, value(accountId, purpose), properties.otpTtl());
        redis.opsForValue().set(index, hash, properties.otpTtl());
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

    /** Lấy và xóa ticket trong một lệnh; {@code true} nếu ticket còn và đúng tài khoản, mục đích (dùng một lần). */
    public boolean take(String ticket, OtpPurpose purpose, UUID accountId) {
        if (ticket == null || ticket.isBlank()) {
            return false;
        }
        String hash = Hashing.sha256Hex(ticket);
        String value = redis.opsForValue().getAndDelete(PREFIX + hash);
        String index = indexKey(accountId, purpose);
        if (hash.equals(redis.opsForValue().get(index))) {
            redis.delete(index);
        }
        return value(accountId, purpose).equals(value);
    }

    /** Thu hồi ticket đang chờ của tài khoản cho mục đích này. */
    public void revoke(UUID accountId, OtpPurpose purpose) {
        String previous = redis.opsForValue().getAndDelete(indexKey(accountId, purpose));
        if (previous != null) {
            redis.delete(PREFIX + previous);
        }
    }

    /** Vô hiệu hóa tài khoản: thu hồi ticket của mọi mục đích (BR-U01-71). */
    public void revokeAll(UUID accountId) {
        for (OtpPurpose purpose : OtpPurpose.values()) {
            revoke(accountId, purpose);
        }
    }

    static String indexKey(UUID accountId, OtpPurpose purpose) {
        return ACCOUNT_PREFIX + accountId + ":" + purpose.name();
    }

    private static String value(UUID accountId, OtpPurpose purpose) {
        return purpose.name() + ":" + accountId;
    }
}
