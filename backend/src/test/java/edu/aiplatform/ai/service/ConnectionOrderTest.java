package edu.aiplatform.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.ai.model.AiConnection;
import edu.aiplatform.ai.model.AiConnection.EncryptedKey;
import edu.aiplatform.ai.model.AiConnection.Scope;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Thứ tự thử khóa (yêu cầu 2026-11-05): Claude, GPT, Gemini; mô hình tốt trước; bỏ khóa đã tắt. */
class ConnectionOrderTest {

    private static final Instant NOW = Instant.parse("2026-11-05T03:00:00Z");
    private final ProviderCatalog catalog = mock(ProviderCatalog.class);

    private static AiConnection key(AiProvider provider, String model) {
        return AiConnection.create(UUID.randomUUID(), Scope.INSTITUTIONAL, null, provider, model,
                new EncryptedKey(new byte[] {1}, new byte[] {1}, 1), "abcd", NOW);
    }

    @Test
    void claudeThenGptThenGeminiAndBetterModelsFirst() {
        List<String> models = List.of("claude-best", "claude-worse");
        when(catalog.models(AiProvider.CLAUDE)).thenReturn(models);
        AiConnection gemini = key(AiProvider.GEMINI, "gemini-x");
        AiConnection gpt = key(AiProvider.OPENAI, "gpt-x");
        AiConnection claudeWorse = key(AiProvider.CLAUDE, models.get(models.size() - 1));
        AiConnection claudeBetter = key(AiProvider.CLAUDE, models.get(0));
        AiConnection off = key(AiProvider.CLAUDE, models.get(0)).withStatus(AiConnection.Status.DISABLED, NOW);

        assertThat(ConnectionOrder.active(List.of(gemini, off, claudeWorse, gpt, claudeBetter), catalog))
                .containsExactly(claudeBetter, claudeWorse, gpt, gemini);
    }
}
