package edu.aiplatform.files.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/** Download token (P4): 32 byte ngẫu nhiên, Redis lưu bản băm 5 phút, gắn {@code accountId} (BR-U03-21). */
@Service
public class DownloadTokenService {

    public static final Duration TTL = Duration.ofMinutes(5);
    static final String KEY_PREFIX = "file:download-token:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final SecureRandom random = new SecureRandom();

    public DownloadTokenService(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public record Grant(String fileId, UUID accountId) {
    }

    public String issue(String fileId, UUID accountId) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        try {
            redis.opsForValue().set(key(token), objectMapper.writeValueAsString(new Grant(fileId, accountId)), TTL);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
        return token;
    }

    public Optional<Grant> resolve(String token) {
        String value = redis.opsForValue().get(key(token));
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(value, Grant.class));
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }

    static String key(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.US_ASCII));
            return KEY_PREFIX + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
