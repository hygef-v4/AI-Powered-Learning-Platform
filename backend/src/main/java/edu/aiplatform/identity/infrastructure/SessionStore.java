package edu.aiplatform.identity.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.identity.application.IdentityProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Phiên đăng nhập trên Redis (infrastructure-design U01 mục 3, P1). Access token mang {@code sid}; xóa
 * {@code session:{sid}} là thu hồi. Refresh token chỉ lưu băm; mỗi lần refresh xoay token mới, token cũ được đánh dấu
 * "đã dùng" tới hết hạn để phát hiện dùng lại thì thu hồi cả phiên (BR-U01-46). Idle 2 giờ bằng TTL làm mới mỗi lần
 * refresh, tối đa 7 ngày tính từ {@code issuedAt} (BR-U01-44). Lỗi Redis để nổi lên ({@code RedisConnectionFailureException}).
 */
@Component
public class SessionStore {

    static final String SESSION_PREFIX = "session:";
    static final String REFRESH_PREFIX = "session:refresh:";
    static final String ACCOUNT_SESSIONS_PREFIX = "account:sessions:";
    static final String USED_MARK = "used:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final Duration idleTtl;
    private final Duration maxTtl;

    public SessionStore(StringRedisTemplate redis, ObjectMapper objectMapper, Clock clock,
                        IdentityProperties properties) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.idleTtl = properties.refreshIdleTtl();
        this.maxTtl = properties.refreshMaxTtl();
    }

    /** Dữ liệu phiên lưu ở {@code session:{sid}}. */
    public record SessionRecord(UUID accountId, Instant issuedAt, Instant lastUsedAt, String refreshHash) {
    }

    /** Phiên mới: {@code refreshToken} là bản rõ, chỉ trả cho cookie, không lưu. */
    public record IssuedSession(String sid, String refreshToken) {
    }

    public sealed interface IRotateResult {
    }

    public record Rotated(String sid, UUID accountId, String refreshToken) implements IRotateResult {
    }

    public record Rejected(Reason reason) implements IRotateResult {
    }

    public enum Reason {
        UNKNOWN, EXPIRED, REUSED
    }

    public IssuedSession create(UUID accountId) {
        Instant now = clock.instant();
        String sid = UUID.randomUUID().toString();
        String refreshToken = Hashing.randomToken(32);
        String refreshHash = Hashing.sha256Hex(refreshToken);
        write(sid, new SessionRecord(accountId, now, now, refreshHash), idleTtl);
        redis.opsForValue().set(REFRESH_PREFIX + refreshHash, sid, idleTtl);
        String accountKey = ACCOUNT_SESSIONS_PREFIX + accountId;
        redis.opsForSet().add(accountKey, sid);
        redis.expire(accountKey, maxTtl);
        return new IssuedSession(sid, refreshToken);
    }

    /** Phiên còn hiệu lực (chưa xóa, chưa quá 7 ngày); mỗi request đã xác thực đều gọi (BR-U01-44). */
    public Optional<SessionRecord> find(String sid) {
        Optional<SessionRecord> session = read(sid);
        if (session.isPresent() && remaining(session.get().issuedAt(), clock.instant()).isZero()) {
            delete(sid);
            return Optional.empty();
        }
        return session;
    }

    /** Xoay refresh token (NFR-U01-11). {@code getAndDelete} nguyên tử: hai request cùng token chỉ một cái thắng. */
    public IRotateResult rotate(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return new Rejected(Reason.UNKNOWN);
        }
        String hash = Hashing.sha256Hex(refreshToken);
        String value = redis.opsForValue().getAndDelete(REFRESH_PREFIX + hash);
        if (value == null) {
            return new Rejected(Reason.UNKNOWN);
        }
        if (value.startsWith(USED_MARK)) {
            delete(value.substring(USED_MARK.length()));
            return new Rejected(Reason.REUSED);
        }
        String sid = value;
        Optional<SessionRecord> current = read(sid);
        if (current.isEmpty()) {
            return new Rejected(Reason.UNKNOWN);
        }
        SessionRecord session = current.get();
        if (!hash.equals(session.refreshHash())) {
            delete(sid);
            return new Rejected(Reason.REUSED);
        }
        Instant now = clock.instant();
        Duration ttl = min(idleTtl, remaining(session.issuedAt(), now));
        if (ttl.isZero()) {
            delete(sid);
            return new Rejected(Reason.EXPIRED);
        }
        String newToken = Hashing.randomToken(32);
        String newHash = Hashing.sha256Hex(newToken);
        write(sid, new SessionRecord(session.accountId(), session.issuedAt(), now, newHash), ttl);
        redis.opsForValue().set(REFRESH_PREFIX + newHash, sid, ttl);
        redis.opsForValue().set(REFRESH_PREFIX + hash, USED_MARK + sid, ttl);
        return new Rotated(sid, session.accountId(), newToken);
    }

    /** Đăng xuất: chỉ phiên này (BR-U01-45). */
    public void delete(String sid) {
        Optional<SessionRecord> session = read(sid);
        redis.delete(SESSION_PREFIX + sid);
        session.ifPresent(s -> {
            redis.delete(REFRESH_PREFIX + s.refreshHash());
            redis.opsForSet().remove(ACCOUNT_SESSIONS_PREFIX + s.accountId(), sid);
        });
    }

    /** Đặt lại mật khẩu, đổi role, vô hiệu hóa: xóa mọi phiên (BR-U01-34, 63, 71). */
    public void deleteAll(UUID accountId) {
        deleteAllExcept(accountId, null);
    }

    /** Đổi mật khẩu: xóa mọi phiên khác, giữ phiên hiện tại (BR-U01-34). */
    public void deleteAllExcept(UUID accountId, String keepSid) {
        Set<String> sids = redis.opsForSet().members(ACCOUNT_SESSIONS_PREFIX + accountId);
        if (sids == null) {
            return;
        }
        for (String sid : sids) {
            if (!sid.equals(keepSid)) {
                delete(sid);
            }
        }
    }

    /** Thời gian còn lại tới trần 7 ngày; không âm. */
    Duration remaining(Instant issuedAt, Instant now) {
        Duration left = Duration.between(now, issuedAt.plus(maxTtl));
        return left.isNegative() ? Duration.ZERO : left;
    }

    private static Duration min(Duration a, Duration b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    private Optional<SessionRecord> read(String sid) {
        String json = redis.opsForValue().get(SESSION_PREFIX + sid);
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, SessionRecord.class));
        } catch (JsonProcessingException e) {
            redis.delete(SESSION_PREFIX + sid);
            return Optional.empty();
        }
    }

    private void write(String sid, SessionRecord session, Duration ttl) {
        try {
            redis.opsForValue().set(SESSION_PREFIX + sid, objectMapper.writeValueAsString(session), ttl);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không ghi được phiên", e);
        }
    }
}
