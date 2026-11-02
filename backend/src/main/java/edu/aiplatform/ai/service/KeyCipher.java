package edu.aiplatform.ai.service;

import edu.aiplatform.ai.config.AiProperties;

import edu.aiplatform.ai.model.AiConnection.EncryptedKey;
import edu.aiplatform.shared.error.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * Mã hóa key provider bằng AES-256-GCM, IV 12 byte ngẫu nhiên mỗi lần, ghi phiên bản khóa để đổi khóa dần (P4,
 * NFR-U06-30). Khóa lấy từ biến môi trường; chưa cấu hình thì từ chối lưu và dùng key (fail closed).
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

    /** Mã hóa bằng khóa mới nhất. */
    public EncryptedKey encrypt(String plain) {
        Map.Entry<Integer, String> current = encodedKeys.lastEntry();
        if (current == null) {
            throw notConfigured();
        }
        byte[] iv = new byte[IV_BYTES];
        random.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(current.getValue()), new GCMParameterSpec(TAG_BITS, iv));
            return new EncryptedKey(cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8)), iv, current.getKey());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Không mã hóa được key", e);
        }
    }

    /** Giải mã ngay trước khi gọi provider; không giữ bản rõ trong cache hay log. */
    public String decrypt(EncryptedKey key) {
        String encoded = encodedKeys.get(key.version());
        if (encoded == null || key.ciphertext() == null) {
            throw notConfigured();
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(encoded), new GCMParameterSpec(TAG_BITS, key.iv()));
            return new String(cipher.doFinal(key.ciphertext()), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Không giải mã được key", e);
        }
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
