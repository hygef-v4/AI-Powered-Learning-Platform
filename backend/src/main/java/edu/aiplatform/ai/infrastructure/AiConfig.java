package edu.aiplatform.ai.infrastructure;

import edu.aiplatform.ai.application.AiProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Nạp cấu hình {@code platform.ai} của U06. */
@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiConfig {
}
