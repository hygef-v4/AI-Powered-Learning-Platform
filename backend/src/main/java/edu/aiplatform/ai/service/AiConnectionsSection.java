package edu.aiplatform.ai.service;

import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.ai.model.AiConnection;
import edu.aiplatform.ai.repository.AiConnectionRepository;
import edu.aiplatform.settings.contract.ISettingsSectionProvider;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Thẻ "API AI của trường" trên Setting List (F14): trạng thái và model từng provider, không có key (BR-U06-04). */
@Component
public class AiConnectionsSection implements ISettingsSectionProvider {

    public static final String KEY = "ai-connections";

    private final AiConnectionRepository connections;

    public AiConnectionsSection(AiConnectionRepository connections) {
        this.connections = connections;
    }

    @Override
    public String sectionKey() {
        return KEY;
    }

    @Override
    public String title() {
        return "API AI của trường";
    }

    @Override
    public String summary() {
        Map<AiProvider, AiConnection> byProvider = connections.listInstitutional().stream()
                .filter(c -> c.status() != AiConnection.Status.REVOKED)
                .collect(Collectors.toMap(AiConnection::provider, Function.identity(), (a, b) -> a));
        if (byProvider.isEmpty()) {
            return "Chưa cấu hình";
        }
        return Arrays.stream(AiProvider.values())
                .map(p -> label(p) + ": " + state(byProvider.get(p)))
                .collect(Collectors.joining(" · "));
    }

    private static String state(AiConnection c) {
        if (c == null) {
            return "chưa cấu hình";
        }
        return (c.status() == AiConnection.Status.ACTIVE ? "đang bật" : "đang tắt") + " (" + c.model() + ")";
    }

    static String label(AiProvider provider) {
        return switch (provider) {
            case GEMINI -> "Gemini";
            case OPENAI -> "OpenAI";
            case CLAUDE -> "Claude";
        };
    }
}
