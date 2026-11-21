package edu.aiplatform.files.integration;

import edu.aiplatform.files.repository.UnattachedFileRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class UnattachedFiles {

    private static final Logger log = LoggerFactory.getLogger(UnattachedFiles.class);
    private static final String LEGACY_KEY = "file:unattached";
    private static final Duration GRACE = Duration.ofMinutes(10);

    private final UnattachedFileRepository repository;
    private final StringRedisTemplate redis;

    public UnattachedFiles(UnattachedFileRepository repository, StringRedisTemplate redis) {
        this.repository = repository;
        this.redis = redis;
    }

    public void addFile(String fileId, Instant refExpiresAt) {
        repository.saveFile(fileId, refExpiresAt.plus(GRACE));
    }

    public void markAttached(String fileId) {
        if (!repository.removeFile(fileId)) {
            redis.opsForZSet().remove(LEGACY_KEY, fileId);
        }
    }

    public List<String> listDue(Instant now, int limit) {
        var due = new LinkedHashSet<>(repository.listDueFiles(now, limit));
        if (due.size() < limit) {
            // Old uploads remain tracked until their original expiry or attachment.
            try {
                var legacy = redis.opsForZSet().rangeByScore(
                        LEGACY_KEY, Double.NEGATIVE_INFINITY, now.getEpochSecond(), 0, limit - due.size());
                if (legacy != null) {
                    due.addAll(legacy);
                }
            } catch (DataAccessException e) {
                log.warn("Chưa đọc được danh sách dọn tệp cũ trong Redis");
            }
        }
        return List.copyOf(due);
    }

    public void forgetFile(String fileId) {
        markAttached(fileId);
    }
}
