package edu.aiplatform.ai.config;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình U06 ({@code platform.ai}): khóa mã hóa key (base64 32 byte, không có giá trị mặc định, SECURITY-09),
 * timeout gọi provider, model cho phép và trần {@code maxOutputTokens} của từng provider (BR-U06-02).
 */
@ConfigurationProperties(prefix = "platform.ai")
public record AiProperties(String keyEncryptionKey, String keyEncryptionKeyV2, Duration requestTimeout,
                           Duration testTimeout, Map<String, Provider> providers) {

    public AiProperties {
        requestTimeout = requestTimeout == null ? Duration.ofSeconds(120) : requestTimeout;
        testTimeout = testTimeout == null ? Duration.ofSeconds(20) : testTimeout;
        providers = providers == null ? Map.of() : providers;
    }

    /**
     * {@code visionModels}: model đọc được ảnh; rỗng thì mọi model trong {@code models} (BR-U06-57).
     * Chỉ giữ constructor chuẩn: thêm constructor khác thì Spring không bind được record này.
     */
    public record Provider(List<String> models, int maxOutputTokens, List<String> visionModels) {

        public Provider {
            models = clean(models);
            visionModels = clean(visionModels);
        }

        private static List<String> clean(List<String> names) {
            return names == null ? List.of() : names.stream().map(String::trim).filter(m -> !m.isEmpty()).toList();
        }
    }
}
