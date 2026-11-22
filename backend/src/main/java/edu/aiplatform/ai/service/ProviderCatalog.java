package edu.aiplatform.ai.service;

import edu.aiplatform.ai.config.AiProperties;

import edu.aiplatform.ai.contract.AiProvider;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Model cho phép và trần {@code maxOutputTokens} theo provider, do server cấu hình (BR-U06-02). */
@Component
public class ProviderCatalog {

    private static final Set<String> REASONING_EFFORTS = Set.of("low", "medium", "high");
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

    /** Model có suy nghĩ tính chung trần đầu ra, theo {@code reasoning-models} của provider. */
    public boolean isReasoningModel(AiProvider provider, String model) {
        AiProperties.Provider config = loadProviderConfig(provider);
        return config != null && model != null && config.reasoningModels().contains(model);
    }

    /**
     * Trần gửi provider: phần trả lời cộng phần chừa cho suy nghĩ (chỉ với model có suy nghĩ), không quá trần của
     * provider. Dùng cho cả giữ quota và lời gọi để hai số luôn khớp.
     */
    public int resolveOutputCap(AiProvider provider, String model, int answerTokens) {
        int reserve = isReasoningModel(provider, model) ? loadProviderConfig(provider).reasoningReserveTokens() : 0;
        return Math.min(answerTokens + reserve, readMaxOutputTokens(provider));
    }

    /** Mức suy nghĩ gửi provider; {@code null} khi model không suy nghĩ hoặc chưa cấu hình mức hợp lệ. */
    public String readReasoningEffort(AiProvider provider, String model) {
        if (!isReasoningModel(provider, model)) {
            return null;
        }
        String effort = loadProviderConfig(provider).reasoningEffort();
        return REASONING_EFFORTS.contains(effort) ? effort : null;
    }

    public int readReasoningReserveTokens(AiProvider provider, String model) {
        return isReasoningModel(provider, model) ? loadProviderConfig(provider).reasoningReserveTokens() : 0;
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
