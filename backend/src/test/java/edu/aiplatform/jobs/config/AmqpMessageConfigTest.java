package edu.aiplatform.jobs.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.jobs.contract.DomainEvent;
import edu.aiplatform.jobs.contract.JobMessage;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;

class AmqpMessageConfigTest {
    private final MessageConverter converter =
            new AmqpMessageConfig().jsonMessageConverter(new ObjectMapper().findAndRegisterModules());

    @Test
    void jobMessageRoundTripsThroughTrustedContractPackage() {
        var job = new JobMessage(1, "EMAIL_SEND", "key-1", Map.of("accountId", "a1"), 0, "c1");

        Message message = converter.toMessage(job, new MessageProperties());

        assertThat(converter.fromMessage(message)).isEqualTo(job);
    }

    @Test
    void domainEventClassIsAlsoTrusted() {
        assertThat(DomainEvent.class.getPackageName())
                .isEqualTo(AmqpMessageConfig.TRUSTED_MESSAGE_PACKAGE);
    }

    @Test
    void classOutsideContractPackageIsRejected() {
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setHeader("__TypeId__", "edu.aiplatform.jobs.config.AmqpMessageConfig");

        assertThatThrownBy(() -> converter.fromMessage(new Message("{}".getBytes(), properties)))
                .hasStackTraceContaining("not in the trusted packages");
    }
}
