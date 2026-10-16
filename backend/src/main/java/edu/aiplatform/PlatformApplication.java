package edu.aiplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * Backend modular monolith của 15 unit. Mỗi unit là một package (hoặc vài package) dưới {@code edu.aiplatform};
 * unit khác chỉ được dùng package {@code contract} của nó.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class PlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlatformApplication.class, args);
    }
}
