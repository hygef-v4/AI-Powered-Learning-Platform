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
                "gemini", new AiProperties.Provider(List.of("gemini-2.5-flash", " gemini-2.5-pro "), 8192, List.of(), List.of(), "", 0),
                "openai", new AiProperties.Provider(List.of("gpt-4.1-mini"), 4096, List.of(), List.of(), "", 0)));
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
                        List.of("gemini-2.5-flash", "gemini-2.5-pro"), 8192, List.of(" gemini-2.5-pro ", ""), List.of(), "", 0))));
        assertThat(narrowed.supportsImages(AiProvider.GEMINI, "gemini-2.5-pro")).isTrue();
        assertThat(narrowed.supportsImages(AiProvider.GEMINI, "gemini-2.5-flash")).isFalse();
    }

    @Test
    void springBindsReasoningSettingsIncludingBlankAndMissingValues() {
        var source = new org.springframework.boot.context.properties.source.MapConfigurationPropertySource(Map.of(
                "platform.ai.providers.gemini.models", "gemini-3.5-flash-lite",
                "platform.ai.providers.gemini.max-output-tokens", "16384",
                "platform.ai.providers.gemini.reasoning-models", "gemini-3.5-flash-lite",
                "platform.ai.providers.gemini.reasoning-effort", "low",
                "platform.ai.providers.gemini.reasoning-reserve-tokens", "4096",
                "platform.ai.providers.claude.models", "claude-haiku-5-5",
                "platform.ai.providers.claude.reasoning-models", ""));
        AiProperties bound = new org.springframework.boot.context.properties.bind.Binder(source)
                .bind("platform.ai", AiProperties.class).get();

        assertThat(bound.providers().get("gemini").reasoningModels()).containsExactly("gemini-3.5-flash-lite");
        assertThat(bound.providers().get("gemini").reasoningEffort()).isEqualTo("low");
        assertThat(bound.providers().get("claude").reasoningModels()).isEmpty();
        assertThat(bound.providers().get("claude").reasoningEffort()).isEmpty();
        assertThat(bound.providers().get("claude").reasoningReserveTokens()).isEqualTo(4096);
    }

    @Test
    void reasoningModelsGetReservedThinkingTokensAndOnlyValidEffortsAreSent() {
        // Thiết kế G3 2026-11-20: trần = trả lời + phần chừa, không quá trần provider; effort sai/rỗng thì không gửi.
        ProviderCatalog catalog = new ProviderCatalog(new AiProperties(KEY_V1, null, Duration.ofSeconds(120),
                Duration.ofSeconds(20), Map.of(
                "gemini", new AiProperties.Provider(List.of("gemini-3.5-flash-lite", "gemini-old"), 16384, List.of(),
                        List.of(" gemini-3.5-flash-lite "), " LOW ", 4096),
                "openai", new AiProperties.Provider(List.of("gpt-5-mini"), 16384, List.of(),
                        List.of("gpt-5-mini"), "", 0),
                "claude", new AiProperties.Provider(List.of("claude-haiku-5-5"), 8192, List.of(),
                        List.of("claude-haiku-5-5"), "extreme", 2000))));

        assertThat(catalog.resolveOutputCap(AiProvider.GEMINI, "gemini-3.5-flash-lite", 6956)).isEqualTo(11052);
        assertThat(catalog.resolveOutputCap(AiProvider.GEMINI, "gemini-3.5-flash-lite", 14000)).isEqualTo(16384);
        assertThat(catalog.resolveOutputCap(AiProvider.GEMINI, "gemini-old", 6956)).as("model không suy nghĩ")
                .isEqualTo(6956);
        assertThat(catalog.readReasoningEffort(AiProvider.GEMINI, "gemini-3.5-flash-lite")).isEqualTo("low");
        assertThat(catalog.readReasoningEffort(AiProvider.GEMINI, "gemini-old")).isNull();
        assertThat(catalog.readReasoningReserveTokens(AiProvider.GEMINI, "gemini-old")).isZero();

        assertThat(catalog.readReasoningEffort(AiProvider.OPENAI, "gpt-5-mini")).as("chỉ chừa token").isNull();
        assertThat(catalog.resolveOutputCap(AiProvider.OPENAI, "gpt-5-mini", 6956))
                .as("không đặt phần chừa thì dùng mặc định 4096").isEqualTo(11052);
        assertThat(catalog.readReasoningEffort(AiProvider.CLAUDE, "claude-haiku-5-5")).as("effort lạ").isNull();
        assertThat(catalog.resolveOutputCap(AiProvider.CLAUDE, "claude-haiku-5-5", 6956)).isEqualTo(8192);
    }
}
