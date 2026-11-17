package edu.aiplatform.ai.service;

import edu.aiplatform.ai.config.AiProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.ai.model.AiConnection;
import edu.aiplatform.ai.model.AiConnection.EncryptedKey;
import edu.aiplatform.ai.model.AiConnection.Scope;
import edu.aiplatform.shared.error.ApiException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class KeyCipherAndCatalogTest {

    static final String KEY_V1 = Base64.getEncoder().encodeToString(new byte[32]);
    static final String KEY_V2 = Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());

    static AiProperties properties(String v1, String v2) {
        return new AiProperties(v1, v2, Duration.ofSeconds(120), Duration.ofSeconds(20), Map.of(
                "gemini", new AiProperties.Provider(List.of("gemini-2.5-flash", " gemini-2.5-pro "), 8192, List.of()),
                "openai", new AiProperties.Provider(List.of("gpt-4.1-mini"), 4096, List.of())));
    }

    @Test
    void encryptsWithRandomIvAndDecryptsBack() {
        KeyCipher cipher = new KeyCipher(properties(KEY_V1, null));
        EncryptedKey a = cipher.encryptKey("sk-secret-3kQf");
        EncryptedKey b = cipher.encryptKey("sk-secret-3kQf");
        assertThat(a.ciphertext()).isNotEqualTo("sk-secret-3kQf".getBytes());
        assertThat(a.iv()).isNotEqualTo(b.iv());
        assertThat(a.version()).isEqualTo(1);
        assertThat(cipher.decryptKey(a)).isEqualTo("sk-secret-3kQf");
    }

    @Test
    void newKeysUseLatestVersionAndOldOnesStillDecrypt() {
        EncryptedKey old = new KeyCipher(properties(KEY_V1, null)).encryptKey("old-key");
        KeyCipher rotated = new KeyCipher(properties(KEY_V1, KEY_V2));
        assertThat(rotated.encryptKey("new-key").version()).isEqualTo(2);
        assertThat(rotated.decryptKey(old)).isEqualTo("old-key");
    }

    @Test
    void connectionKeyOnlyDecryptsForTheConnectionItWasSealedFor() {
        KeyCipher cipher = new KeyCipher(properties(KEY_V1, null));
        UUID id = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        EncryptedKey sealed = cipher.encryptConnectionKey(id, Scope.PERSONAL, owner, AiProvider.GEMINI, "AIza-own-3kQf");

        assertThat(cipher.decryptConnectionKey(connection(id, Scope.PERSONAL, owner, AiProvider.GEMINI, sealed)))
                .isEqualTo("AIza-own-3kQf");
        assertThatThrownBy(() -> cipher.decryptConnectionKey(
                connection(UUID.randomUUID(), Scope.PERSONAL, owner, AiProvider.GEMINI, sealed)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> cipher.decryptConnectionKey(
                connection(id, Scope.PERSONAL, UUID.randomUUID(), AiProvider.GEMINI, sealed)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> cipher.decryptConnectionKey(
                connection(id, Scope.INSTITUTIONAL, null, AiProvider.GEMINI, sealed)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> cipher.decryptConnectionKey(
                connection(id, Scope.PERSONAL, owner, AiProvider.OPENAI, sealed)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void legacyUnboundCiphertextIsRejectedForConnections() {
        KeyCipher cipher = new KeyCipher(properties(KEY_V1, null));
        EncryptedKey legacy = cipher.encryptKey("AIza-legacy-0000");

        assertThatThrownBy(() -> cipher.decryptConnectionKey(
                connection(UUID.randomUUID(), Scope.INSTITUTIONAL, null, AiProvider.GEMINI, legacy)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(cipher.decryptKey(legacy)).isEqualTo("AIza-legacy-0000");
    }

    static AiConnection connection(UUID id, Scope scope, UUID owner, AiProvider provider, EncryptedKey key) {
        return AiConnection.createConnection(id, scope, owner, provider, "gemini-2.5-flash", key, "0000",
                Instant.parse("2026-11-11T00:00:00Z"));
    }

    @Test
    void missingEncryptionKeyFailsClosed() {
        KeyCipher cipher = new KeyCipher(properties(null, " "));
        assertThat(cipher.hasKeys()).isFalse();
        assertThatThrownBy(() -> cipher.encryptKey("x"))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.readCode()).isEqualTo("AI_KEY_ENCRYPTION_NOT_CONFIGURED"));
    }

    @Test
    void wrongKeyLengthIsRejected() {
        KeyCipher cipher = new KeyCipher(properties(Base64.getEncoder().encodeToString(new byte[16]), null));
        assertThatThrownBy(() -> cipher.encryptKey("x")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void hintShowsOnlyLastFourCharacters() {
        assertThat(KeyCipher.buildHint("  sk-proj-abcdef3kQf ")).isEqualTo("3kQf");
        assertThat(KeyCipher.buildHint("ab")).isEqualTo("ab");
    }

    @Test
    void catalogAllowsOnlyConfiguredModels() {
        ProviderCatalog catalog = new ProviderCatalog(properties(KEY_V1, null));
        assertThat(catalog.isAllowed(AiProvider.GEMINI, "gemini-2.5-pro")).isTrue();
        assertThat(catalog.isAllowed(AiProvider.GEMINI, "gemini-1.0")).isFalse();
        assertThat(catalog.isAllowed(AiProvider.CLAUDE, "claude-sonnet-5-5")).as("chưa cấu hình").isFalse();
        assertThat(catalog.readMaxOutputTokens(AiProvider.OPENAI)).isEqualTo(4096);
        assertThat(catalog.listProviders()).hasSize(3);
    }

    @Test
    void visionModelsDefaultToAllAllowedModelsAndCanBeNarrowed() {
        ProviderCatalog all = new ProviderCatalog(properties(KEY_V1, null));
        assertThat(all.supportsImages(AiProvider.GEMINI, "gemini-2.5-flash")).isTrue();
        assertThat(all.supportsImages(AiProvider.GEMINI, "gemini-1.0")).as("không cho phép").isFalse();

        ProviderCatalog narrowed = new ProviderCatalog(new AiProperties(KEY_V1, null, Duration.ofSeconds(120),
                Duration.ofSeconds(20), Map.of("gemini", new AiProperties.Provider(
                        List.of("gemini-2.5-flash", "gemini-2.5-pro"), 8192, List.of(" gemini-2.5-pro ", "")))));
        assertThat(narrowed.supportsImages(AiProvider.GEMINI, "gemini-2.5-pro")).isTrue();
        assertThat(narrowed.supportsImages(AiProvider.GEMINI, "gemini-2.5-flash")).isFalse();
    }
}
