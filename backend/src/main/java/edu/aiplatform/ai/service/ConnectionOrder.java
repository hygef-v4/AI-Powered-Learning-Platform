package edu.aiplatform.ai.service;

import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.ai.model.AiConnection;
import edu.aiplatform.ai.model.AiConnection.Status;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * Thứ tự thử các khóa (yêu cầu 2026-11-05): Claude, rồi GPT (OpenAI), rồi Gemini; cùng nhà cung cấp thì mô hình tốt
 * trước (thứ tự trong danh mục mô hình của server, xếp từ tốt tới kém), rồi khóa thêm sớm hơn. Chỉ khóa đang bật.
 */
final class ConnectionOrder {

    static final List<AiProvider> PROVIDERS = List.of(AiProvider.CLAUDE, AiProvider.OPENAI, AiProvider.GEMINI);

    private ConnectionOrder() {
    }

    static List<AiConnection> filterActive(List<AiConnection> connections, ProviderCatalog catalog) {
        return connections.stream().filter(c -> c.status() == Status.ACTIVE)
                .sorted(Comparator.<AiConnection>comparingInt(c -> rankProvider(c.provider()))
                        .thenComparingInt(c -> rankModel(catalog, c))
                        .thenComparing(c -> c.createdAt() == null ? Instant.EPOCH : c.createdAt())
                        .thenComparing(c -> c.id().toString()))
                .toList();
    }

    static int rankProvider(AiProvider provider) {
        int at = PROVIDERS.indexOf(provider);
        return at < 0 ? PROVIDERS.size() : at;
    }

    private static int rankModel(ProviderCatalog catalog, AiConnection connection) {
        int at = catalog.listModels(connection.provider()).indexOf(connection.model());
        return at < 0 ? Integer.MAX_VALUE : at;
    }
}
