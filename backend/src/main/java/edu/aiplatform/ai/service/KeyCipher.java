package edu.aiplatform.ai.service;

import edu.aiplatform.ai.config.AiProperties;
import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.ai.model.AiConnection;
import edu.aiplatform.ai.model.AiConnection.EncryptedKey;
import edu.aiplatform.ai.model.AiConnection.Scope;
import edu.aiplatform.shared.error.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * Mã hóa key provider bằng AES-256-GCM, IV 12 byte ngẫu nhiên mỗi lần, ghi phiên bản khóa để đổi khóa dần (P4,
 * NFR-U06-30). Khóa lấy từ biến môi trường; chưa cấu hình thì từ chối lưu và dùng key (fail closed).
 *
 * <p>Khóa API của kết nối được gắn AAD theo id, phạm vi, chủ và nhà cung cấp của kết nối: chép bản mã sang dòng khác
 * thì không giải mã được, nên người có quyền ghi database không dùng được khóa của người khác qua tài khoản mình.
 */
@Component
public class KeyCipher {

    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private final SecureRandom random = new SecureRandom();
    private final TreeMap<Integer, String> encodedKeys = new TreeMap<>();

    public KeyCipher(AiProperties properties) {
        put(1, properties.keyEncryptionKey());
        put(2, properties.keyEncryptionKeyV2());
    }

    private void put(int version, String encoded) {
        if (encoded != null && !encoded.isBlank()) {
            encodedKeys.put(version, encoded.trim());
        }
    }

    public boolean configured() {
        return !encodedKeys.isEmpty();
    }

    /** Mã hóa khóa API của một kết nối, gắn với danh tính của kết nối đó. */
    public EncryptedKey encryptConnectionKey(UUID id, Scope scope, UUID ownerId, AiProvider provider, String plain) {
        return encrypt(plain, connectionContext(id, scope, ownerId, provider));
    }

    /** Giải mã khóa API của kết nối ngay trước khi gọi provider; bản mã của dòng khác hoặc bản mã cũ chưa gắn bị từ chối. */
    public String decryptConnectionKey(AiConnection connection) {
        return decrypt(connection.encryptedKey(), connectionContext(connection.id(), connection.scope(),
                connection.ownerId(), connection.provider()));
    }

    /** Mã hóa bằng khóa mới nhất, không AAD (token MCP). */
    public EncryptedKey encrypt(String plain) {
        return encrypt(plain, null);
    }

    /** Giải mã bản mã không AAD (token MCP, khóa kết nối cũ trước khi gắn). */
    public String decrypt(EncryptedKey key) {
        return decrypt(key, null);
    }

    private EncryptedKey encrypt(String plain, byte[] aad) {
        Map.Entry<Integer, String> current = encodedKeys.lastEntry();
        if (current == null) {
            throw notConfigured();
        }
        byte[] iv = new byte[IV_BYTES];
        random.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(current.getValue()), new GCMParameterSpec(TAG_BITS, iv));
            if (aad != null) {
                cipher.updateAAD(aad);
            }
            return new EncryptedKey(cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8)), iv, current.getKey());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Không mã hóa được key", e);
        }
    }

    /** Không giữ bản rõ trong cache hay log. */
    private String decrypt(EncryptedKey key, byte[] aad) {
        String encoded = encodedKeys.get(key.version());
        if (encoded == null || key.ciphertext() == null) {
            throw notConfigured();
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(encoded), new GCMParameterSpec(TAG_BITS, key.iv()));
            if (aad != null) {
                cipher.updateAAD(aad);
            }
            return new String(cipher.doFinal(key.ciphertext()), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Không giải mã được key", e);
        }
    }

    static byte[] connectionContext(UUID id, Scope scope, UUID ownerId, AiProvider provider) {
        String context = "ai_connection|v1|" + id + "|" + scope.name() + "|" + (ownerId == null ? "-" : ownerId)
                + "|" + provider.name();
        return context.getBytes(StandardCharsets.UTF_8);
    }

    /** 4 ký tự cuối để hiện thay cho key (BR-U06-03). */
    public static String hint(String plain) {
        String trimmed = plain.trim();
        return trimmed.length() <= 4 ? trimmed : trimmed.substring(trimmed.length() - 4);
    }

    private static SecretKey key(String encoded) {
        byte[] bytes = Base64.getDecoder().decode(encoded);
        if (bytes.length != 32) {
            throw new IllegalStateException("Khóa mã hóa key AI phải là 32 byte base64");
        }
        return new SecretKeySpec(bytes, "AES");
    }

    private static ApiException notConfigured() {
        return ApiException.unavailable("AI_KEY_ENCRYPTION_NOT_CONFIGURED",
                "Hệ thống chưa cấu hình khóa mã hóa cho key AI, liên hệ quản trị viên");
    }
}
