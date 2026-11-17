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

    public List<String> listModels(AiProvider provider) {
        AiProperties.Provider config = loadProviderConfig(provider);
        return config == null ? List.of() : config.models();
    }

    public boolean isAllowed(AiProvider provider, String model) {
        return model != null && listModels(provider).contains(model);
    }

    /** Model đọc được ảnh: theo {@code vision-models}, không đặt thì mọi model cho phép (BR-U06-57). */
    public boolean supportsImages(AiProvider provider, String model) {
        if (!isAllowed(provider, model)) {
            return false;
        }
        List<String> vision = loadProviderConfig(provider).visionModels();
        return vision.isEmpty() || vision.contains(model);
    }

    public int readMaxOutputTokens(AiProvider provider) {
        AiProperties.Provider config = loadProviderConfig(provider);
        return config == null || config.maxOutputTokens() <= 0 ? 4096 : config.maxOutputTokens();
    }

    public List<ProviderInfo> listProviders() {
        return Arrays.stream(AiProvider.values())
                .map(p -> new ProviderInfo(p, listModels(p), readMaxOutputTokens(p)))
                .toList();
    }

    private AiProperties.Provider loadProviderConfig(AiProvider provider) {
        return properties.providers().get(provider.name().toLowerCase(Locale.ROOT));
    }

    public record ProviderInfo(AiProvider provider, List<String> models, int maxOutputTokens) {
    }
}
