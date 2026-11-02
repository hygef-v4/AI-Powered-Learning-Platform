package edu.aiplatform.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.ai.domain.AiConnection.EncryptedKey;
import edu.aiplatform.shared.error.ApiException;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class KeyCipherAndCatalogTest {

    static final String KEY_V1 = Base64.getEncoder().encodeToString(new byte[32]);
    static final String KEY_V2 = Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());

    static AiProperties properties(String v1, String v2) {
        return new AiProperties(v1, v2, Duration.ofSeconds(120), Duration.ofSeconds(20), Map.of(
                "gemini", new AiProperties.Provider(List.of("gemini-2.5-flash", " gemini-2.5-pro "), 8192),
                "openai", new AiProperties.Provider(List.of("gpt-4.1-mini"), 4096)));
    }

    @Test
    void encryptsWithRandomIvAndDecryptsBack() {
        KeyCipher cipher = new KeyCipher(properties(KEY_V1, null));
        EncryptedKey a = cipher.encrypt("sk-secret-3kQf");
        EncryptedKey b = cipher.encrypt("sk-secret-3kQf");
        assertThat(a.ciphertext()).isNotEqualTo("sk-secret-3kQf".getBytes());
        assertThat(a.iv()).isNotEqualTo(b.iv());
        assertThat(a.version()).isEqualTo(1);
        assertThat(cipher.decrypt(a)).isEqualTo("sk-secret-3kQf");
    }

    @Test
    void newKeysUseLatestVersionAndOldOnesStillDecrypt() {
        EncryptedKey old = new KeyCipher(properties(KEY_V1, null)).encrypt("old-key");
        KeyCipher rotated = new KeyCipher(properties(KEY_V1, KEY_V2));
        assertThat(rotated.encrypt("new-key").version()).isEqualTo(2);
        assertThat(rotated.decrypt(old)).isEqualTo("old-key");
    }

    @Test
    void missingEncryptionKeyFailsClosed() {
        KeyCipher cipher = new KeyCipher(properties(null, " "));
        assertThat(cipher.configured()).isFalse();
        assertThatThrownBy(() -> cipher.encrypt("x"))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("AI_KEY_ENCRYPTION_NOT_CONFIGURED"));
    }

    @Test
    void wrongKeyLengthIsRejected() {
        KeyCipher cipher = new KeyCipher(properties(Base64.getEncoder().encodeToString(new byte[16]), null));
        assertThatThrownBy(() -> cipher.encrypt("x")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void hintShowsOnlyLastFourCharacters() {
        assertThat(KeyCipher.hint("  sk-proj-abcdef3kQf ")).isEqualTo("3kQf");
        assertThat(KeyCipher.hint("ab")).isEqualTo("ab");
    }

    @Test
    void catalogAllowsOnlyConfiguredModels() {
        ProviderCatalog catalog = new ProviderCatalog(properties(KEY_V1, null));
        assertThat(catalog.allowed(AiProvider.GEMINI, "gemini-2.5-pro")).isTrue();
        assertThat(catalog.allowed(AiProvider.GEMINI, "gemini-1.0")).isFalse();
        assertThat(catalog.allowed(AiProvider.CLAUDE, "claude-sonnet-5-5")).as("chưa cấu hình").isFalse();
        assertThat(catalog.maxOutputTokens(AiProvider.OPENAI)).isEqualTo(4096);
        assertThat(catalog.providers()).hasSize(3);
    }
}
