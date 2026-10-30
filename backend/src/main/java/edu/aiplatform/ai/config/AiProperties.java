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

    public record Provider(List<String> models, int maxOutputTokens) {

        public Provider {
            models = models == null ? List.of() : models.stream().map(String::trim).filter(m -> !m.isEmpty()).toList();
        }
    }
}
