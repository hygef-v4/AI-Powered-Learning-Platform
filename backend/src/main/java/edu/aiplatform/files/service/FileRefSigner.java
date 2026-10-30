package edu.aiplatform.files.service;

import edu.aiplatform.files.config.FilesProperties;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.files.contract.ArtifactPurpose;
import edu.aiplatform.files.contract.FileRef;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Ký và kiểm {@code FileRef}: payload JSON (base64url) và HMAC-SHA256 (BR-U03-30). Payload có đủ thuộc tính tệp
 * nên {@code attach} không phải gọi Drive.
 */
@Component
public class FileRefSigner {

    private static final Logger log = LoggerFactory.getLogger(FileRefSigner.class);
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final byte[] secret;
    private final ObjectMapper objectMapper;

    public FileRefSigner(FilesProperties properties, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        if (properties.filerefSecret() == null || properties.filerefSecret().isBlank()) {
            log.warn("U03_FILEREF_SECRET chưa đặt: dùng khóa ngẫu nhiên, FileRef mất hiệu lực khi khởi động lại");
            this.secret = new byte[32];
            new SecureRandom().nextBytes(this.secret);
        } else {
            this.secret = properties.filerefSecret().getBytes(StandardCharsets.UTF_8);
        }
    }

    public record Payload(String fileId, UUID ownerAccountId, ArtifactPurpose purpose, String mediaType, long byteSize,
                          String originalFileName, String sha256, long expiresAtEpochSecond) {

        public Instant expiresAt() {
            return Instant.ofEpochSecond(expiresAtEpochSecond);
        }
    }

    public FileRef sign(Payload payload) {
        try {
            String body = ENCODER.encodeToString(objectMapper.writeValueAsBytes(payload));
            return new FileRef(body + "." + ENCODER.encodeToString(mac(body)));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không ký được FileRef", e);
        }
    }

    /** Trả payload nếu chữ ký đúng; không kiểm hạn (người gọi kiểm). */
    public Optional<Payload> verify(FileRef fileRef) {
        if (fileRef == null || fileRef.value() == null) {
            return Optional.empty();
        }
        String[] parts = fileRef.value().split("\\.", -1);
        if (parts.length != 2) {
            return Optional.empty();
        }
        try {
            if (!MessageDigest.isEqual(mac(parts[0]), DECODER.decode(parts[1]))) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(DECODER.decode(parts[0]), Payload.class));
        } catch (IllegalArgumentException | java.io.IOException e) {
            return Optional.empty();
        }
    }

    private byte[] mac(String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(body.getBytes(StandardCharsets.US_ASCII));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
