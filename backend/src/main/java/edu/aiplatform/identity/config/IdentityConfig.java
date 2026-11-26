package edu.aiplatform.identity.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Nạp cấu hình {@code platform.identity} và bộ băm mật khẩu của U01. */
@Configuration
@EnableConfigurationProperties(IdentityProperties.class)
public class IdentityConfig {

    /**
     * bcrypt, cost cấu hình được, mặc định 12 (BR-U01-32, NFR-U01-14). Số lần băm cùng lúc mặc định bằng số CPU JVM
     * được cấp ({@code 0} = tự lấy), lần thừa chờ tối đa {@code bcrypt-queue-wait} (TC-PERF-002).
     */
    @Bean
    PasswordEncoder passwordEncoder(IdentityProperties properties,
                                    @Value("${platform.identity.bcrypt-max-concurrent:0}") int maxConcurrent,
                                    @Value("${platform.identity.bcrypt-queue-wait:30s}") Duration queueWait) {
        int permits = maxConcurrent > 0 ? maxConcurrent : Runtime.getRuntime().availableProcessors();
        return new ConcurrencyLimitedPasswordEncoder(new BCryptPasswordEncoder(properties.bcryptCost()), permits,
                queueWait);
    }
}
