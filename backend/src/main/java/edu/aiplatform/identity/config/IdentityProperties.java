package edu.aiplatform.identity.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình U01 (logical-components mục 4). Ngưỡng là cấu hình, không cố định trong code (NFR-U01-23);
 * khóa ký JWT và SMTP chỉ lấy từ biến môi trường (NFR-U01-51).
 *
 * @param allowedEmailDomains tên miền email trường được phép (BR-U01-03)
 * @param cookieSecure chỉ tắt ở profile {@code local} chạy HTTP (P2)
 * @param bootstrapAdminEmail email Admin đầu tiên, tạo ở {@code PENDING} khi chưa có Admin nào
 */
@ConfigurationProperties(prefix = "platform.identity")
public record IdentityProperties(List<String> allowedEmailDomains, String jwtSecret, Duration accessTtl,
                                 Duration refreshIdleTtl, Duration refreshMaxTtl, int bcryptCost, Duration otpTtl,
                                 int otpMaxAttempts, int loginLockThreshold, Duration loginLockDuration,
                                 boolean cookieSecure, String mailFrom, String bootstrapAdminEmail,
                                 String bootstrapAdminName, RateLimit rateLimit) {

    public IdentityProperties {
        allowedEmailDomains = allowedEmailDomains == null ? List.of() : List.copyOf(allowedEmailDomains);
    }

    /**
     * Ngưỡng giới hạn tần suất (NFR-U01-20, 21). {@code loginPerIp} và {@code passwordChangeFailures} chỉ đếm lần sai.
     */
    public record RateLimit(int otpPerEmailPerHour, Duration otpCooldown, int otpPerIpPerHour, int loginPerIp,
                            Duration loginIpWindow, int passwordChangeFailures, Duration passwordChangeWindow) {
    }
}
