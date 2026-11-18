package edu.aiplatform.jobs.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AmqpMessageConfig {
    /** JobMessage và DomainEvent; spring-amqp 3.2.12 chỉ tin java.util/java.lang nếu không khai báo. */
    static final String TRUSTED_MESSAGE_PACKAGE = "edu.aiplatform.jobs.contract";

    /** Message dạng JSON, dùng ObjectMapper chung (có module thời gian). */
    @Bean
    MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper, TRUSTED_MESSAGE_PACKAGE);
    }
}
