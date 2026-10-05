package edu.aiplatform.files.application;

import edu.aiplatform.files.port.DownloadToken;
import edu.aiplatform.shared.web.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Download token (P4): 32 byte ngẫu nhiên, Redis chỉ lưu băm `file:download-token:{sha256}` TTL 5 phút, gắn
 * `accountId`. Dùng lại được trong hạn (trình xem ảnh/PDF tải từng phần). Sai người → 404 và audit (BR-U03-21, 40).
 */
@Service
public class DownloadTokenService {

    static final String KEY_PREFIX = "file:download-token:";
    public static final String DOWNLOAD_PATH = "/api/v1/files/download/";

    private final StringRedisTemplate redis;
    private final FileAudit audit;
    private final Duration ttl;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    @Autowired
    public DownloadTokenService(StringRedisTemplate redis, FileAudit audit, FilesProperties properties) {
        this(redis, audit, properties.downloadTokenTtl(), Clock.systemUTC());
    }

    DownloadTokenService(StringRedisTemplate redis, FileAudit audit, Duration ttl, Clock clock) {
        this.redis = redis;
        this.audit = audit;
        this.ttl = ttl;
        this.clock = clock;
    }

    public DownloadToken issue(String fileId, UUID accountId) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        redis.opsForValue().set(KEY_PREFIX + hash(token), fileId + "|" + accountId, ttl);
        return new DownloadToken(token, DOWNLOAD_PATH + token, clock.instant().plus(ttl));
    }

    /** @return `fileId` của token nếu token còn hạn và thuộc đúng `accountId` */
    public String resolve(String token, UUID accountId) {
        String value = token == null ? null : redis.opsForValue().get(KEY_PREFIX + hash(token));
        if (value == null) {
            throw notFound();
        }
        int sep = value.lastIndexOf('|');
        if (!value.substring(sep + 1).equals(accountId.toString())) {
            audit.downloadWrongAccount(accountId);
            throw notFound();
        }
        return value.substring(0, sep);
    }

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "FILE_NOT_FOUND", "Không tìm thấy tệp hoặc liên kết đã hết hạn.");
    }

    static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
