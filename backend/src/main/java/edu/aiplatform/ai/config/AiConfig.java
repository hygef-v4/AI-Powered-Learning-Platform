package edu.aiplatform.ai.config;

import edu.aiplatform.ai.config.AiProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Nạp cấu hình {@code platform.ai} của U06. */
@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiConfig {
}
