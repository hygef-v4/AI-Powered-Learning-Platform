package edu.aiplatform.ai.service;

import edu.aiplatform.ai.config.AiProperties;

import edu.aiplatform.ai.contract.AiProvider;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/** Model cho phép và trần {@code maxOutputTokens} theo provider, do server cấu hình (BR-U06-02). */
@Component
public class ProviderCatalog {

    private final AiProperties properties;

    public ProviderCatalog(AiProperties properties) {
        this.properties = properties;
    }

    public List<String> models(AiProvider provider) {
        AiProperties.Provider config = config(provider);
        return config == null ? List.of() : config.models();
    }

    public boolean allowed(AiProvider provider, String model) {
        return model != null && models(provider).contains(model);
    }

    public int maxOutputTokens(AiProvider provider) {
        AiProperties.Provider config = config(provider);
        return config == null || config.maxOutputTokens() <= 0 ? 4096 : config.maxOutputTokens();
    }

    public List<ProviderInfo> providers() {
        return Arrays.stream(AiProvider.values())
                .map(p -> new ProviderInfo(p, models(p), maxOutputTokens(p)))
                .toList();
    }

    private AiProperties.Provider config(AiProvider provider) {
        return properties.providers().get(provider.name().toLowerCase(Locale.ROOT));
    }

    public record ProviderInfo(AiProvider provider, List<String> models, int maxOutputTokens) {
    }
}
