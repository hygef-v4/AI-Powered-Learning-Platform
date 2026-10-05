package edu.aiplatform.files.infrastructure;

import edu.aiplatform.files.application.FilesProperties;
import edu.aiplatform.files.domain.FileRefClaims;
import edu.aiplatform.files.port.ArtifactPurpose;
import edu.aiplatform.files.port.FileRef;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Ký và kiểm `FileRef` bằng HMAC-SHA256 với khóa `U03_FILEREF_SECRET` (Bước 14). Xoay khóa bằng cách đổi biến và
 * triển khai lại: `FileRef` cũ (hạn 1 giờ) mất hiệu lực. Không có khóa (chỉ local) thì sinh khóa ngẫu nhiên và cảnh báo.
 */
@Component
public class FileRefSigner {

    private static final Logger log = LoggerFactory.getLogger(FileRefSigner.class);
    private static final String ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64D = Base64.getUrlDecoder();

    private final byte[] key;
    private final Clock clock;

    @Autowired
    public FileRefSigner(FilesProperties properties) {
        this(properties.fileRefSecret(), Clock.systemUTC());
    }

    public FileRefSigner(String secret, Clock clock) {
        this.clock = clock;
        if (secret == null || secret.isBlank()) {
            log.warn("U03_FILEREF_SECRET is not set; using a random key (FileRef invalid after restart)");
            this.key = new byte[32];
            new SecureRandom().nextBytes(this.key);
        } else {
            this.key = secret.getBytes(StandardCharsets.UTF_8);
        }
    }

    public FileRef sign(FileRefClaims claims) {
        String payload = String.join("|", claims.fileId(), claims.ownerAccountId().toString(), claims.purpose().name(),
                Long.toString(claims.expiresAt().getEpochSecond()));
        String body = B64.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return new FileRef(body + "." + B64.encodeToString(mac(body)));
    }

    /** Rỗng khi chữ ký sai, sai định dạng hoặc đã hết hạn. */
    public Optional<FileRefClaims> verify(FileRef fileRef) {
        if (fileRef == null || fileRef.value() == null) {
            return Optional.empty();
        }
        String[] parts = fileRef.value().split("\\.", -1);
        if (parts.length != 2) {
            return Optional.empty();
        }
        try {
            if (!MessageDigest.isEqual(mac(parts[0]), B64D.decode(parts[1]))) {
                return Optional.empty();
            }
            String[] fields = new String(B64D.decode(parts[0]), StandardCharsets.UTF_8).split("\\|", -1);
            if (fields.length != 4) {
                return Optional.empty();
            }
            FileRefClaims claims = new FileRefClaims(fields[0], UUID.fromString(fields[1]),
                    ArtifactPurpose.valueOf(fields[2]), Instant.ofEpochSecond(Long.parseLong(fields[3])));
            return claims.expiresAt().isAfter(clock.instant()) ? Optional.of(claims) : Optional.empty();
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private byte[] mac(String body) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(key, ALGORITHM));
            return mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
