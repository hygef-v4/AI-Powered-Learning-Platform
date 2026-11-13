package edu.aiplatform.files.integration;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Tệp đã tải lên mà chưa được gắn vào đâu (sorted set {@code file:unattached}, điểm là thời điểm được dọn). FileRef hết
 * hạn sau 1 giờ thì không gắn được nữa, nên quá hạn cộng {@link #GRACE} mà chưa gắn là tệp mồ côi: người dùng hủy, đóng
 * trang, xem trước DOCX rồi bỏ. Gắn một lần là bỏ khỏi danh sách, không bao giờ bị dọn.
 */
@Component
public class UnattachedFiles {

    static final String KEY = "file:unattached";
    static final Duration GRACE = Duration.ofMinutes(10);

    private final StringRedisTemplate redis;

    public UnattachedFiles(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void add(String fileId, Instant refExpiresAt) {
        redis.opsForZSet().add(KEY, fileId, refExpiresAt.plus(GRACE).getEpochSecond());
    }

    public void markAttached(String fileId) {
        redis.opsForZSet().remove(KEY, fileId);
    }

    /** Tệp tới hạn dọn, sớm trước. */
    public List<String> due(Instant now, int limit) {
        Set<String> ids = redis.opsForZSet().rangeByScore(KEY, Double.NEGATIVE_INFINITY, now.getEpochSecond(), 0, limit);
        return ids == null ? List.of() : List.copyOf(ids);
    }

    public void forget(String fileId) {
        redis.opsForZSet().remove(KEY, fileId);
    }
}
