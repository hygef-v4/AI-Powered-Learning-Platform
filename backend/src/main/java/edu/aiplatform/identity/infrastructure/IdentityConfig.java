package edu.aiplatform.identity.infrastructure;

import edu.aiplatform.identity.application.IdentityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Nạp cấu hình {@code platform.identity} và bộ băm mật khẩu của U01. */
@Configuration
@EnableConfigurationProperties(IdentityProperties.class)
public class IdentityConfig {

    /** bcrypt, cost cấu hình được, mặc định 12 (BR-U01-32, NFR-U01-14). */
    @Bean
    PasswordEncoder passwordEncoder(IdentityProperties properties) {
        return new BCryptPasswordEncoder(properties.bcryptCost());
    }
}
