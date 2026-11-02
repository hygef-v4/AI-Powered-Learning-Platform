package edu.aiplatform.ai.domain;

import edu.aiplatform.ai.contract.AiProvider;
import java.time.Instant;
import java.util.UUID;

/**
 * Kết nối API tới một provider (domain-entities U06 mục 2). Bản mã key không bao giờ ra khỏi U06; chỉ
 * {@code keyHint} (4 ký tự cuối) được hiện (BR-U06-03).
 */
public record AiConnection(UUID id, Scope scope, UUID ownerId, AiProvider provider, String model, byte[] keyCiphertext,
                           byte[] keyIv, Integer keyVersion, String keyHint, Status status, Instant lastTestAt,
                           String lastTestResult, Instant createdAt, Instant updatedAt, long version) {

    public enum Scope {
        INSTITUTIONAL, PERSONAL
    }

    public enum Status {
        ACTIVE, DISABLED, REVOKED
    }

    public static AiConnection create(UUID id, Scope scope, UUID ownerId, AiProvider provider, String model,
                                      EncryptedKey key, String hint, Instant now) {
        return new AiConnection(id, scope, ownerId, provider, model, key.ciphertext(), key.iv(), key.version(), hint,
                Status.ACTIVE, null, null, now, now, 0);
    }

    public AiConnection withKey(String newModel, EncryptedKey key, String hint, Instant now) {
        return new AiConnection(id, scope, ownerId, provider, newModel, key.ciphertext(), key.iv(), key.version(), hint,
                status, null, null, createdAt, now, version);
    }

    public AiConnection withModel(String newModel, Instant now) {
        return new AiConnection(id, scope, ownerId, provider, newModel, keyCiphertext, keyIv, keyVersion, keyHint, status,
                lastTestAt, lastTestResult, createdAt, now, version);
    }

    public AiConnection withStatus(Status next, Instant now) {
        if (next == Status.DISABLED && scope != Scope.INSTITUTIONAL) {
            throw new IllegalStateException("Chỉ API chung có trạng thái DISABLED");
        }
        return new AiConnection(id, scope, ownerId, provider, model, keyCiphertext, keyIv, keyVersion, keyHint, next,
                lastTestAt, lastTestResult, createdAt, now, version);
    }

    /** Thu hồi xóa bản mã (BR-U06-03). */
    public AiConnection revoked(Instant now) {
        return new AiConnection(id, scope, ownerId, provider, model, null, null, null, keyHint, Status.REVOKED, lastTestAt,
                lastTestResult, createdAt, now, version);
    }

    public AiConnection withTest(String result, Instant at) {
        return new AiConnection(id, scope, ownerId, provider, model, keyCiphertext, keyIv, keyVersion, keyHint, status, at,
                result, createdAt, updatedAt, version);
    }

    public EncryptedKey encryptedKey() {
        return new EncryptedKey(keyCiphertext, keyIv, keyVersion == null ? 0 : keyVersion);
    }

    /** Key đã mã hóa; IV ngẫu nhiên mỗi lần mã hóa. */
    public record EncryptedKey(byte[] ciphertext, byte[] iv, int version) {
    }
}
