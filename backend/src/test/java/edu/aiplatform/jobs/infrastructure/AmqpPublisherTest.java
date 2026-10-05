package edu.aiplatform.jobs.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import edu.aiplatform.jobs.port.DomainEvent;
import edu.aiplatform.jobs.port.JobMessage;
import edu.aiplatform.jobs.port.JobTypes;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/** BR-U03-58, 70; NFR-U03-40, 52; P9. */
class AmqpPublisherTest {

    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final AmqpPublisher publisher = new AmqpPublisher(rabbit);
    private final JobMessage otp = new JobMessage(1, JobTypes.OTP_DELIVERY, "k", Map.of("accountId", "a"), 0, "c");

    @Test
    void brokerDownDoesNotFailTheCaller() {
        doThrow(new AmqpConnectException(new RuntimeException("down"))).when(rabbit)
                .convertAndSend(anyString(), anyString(), any(Object.class), any(MessagePostProcessor.class),
                        any(CorrelationData.class));
        doThrow(new AmqpConnectException(new RuntimeException("down"))).when(rabbit)
                .convertAndSend(anyString(), anyString(), any(Object.class), any(MessagePostProcessor.class));

        assertThatCode(() -> publisher.publishJob(otp)).doesNotThrowAnyException();
        assertThatCode(() -> publisher.publishEvent(new DomainEvent(1, UUID.randomUUID(), "payment.paid", "U07",
                Instant.now(), "c", Map.of()))).doesNotThrowAnyException();
        assertThat(publisher.publishRetry(otp, "jobs.retry.30s")).isFalse();
    }

    @Test
    void slowBrokerDoesNotBlockTheCaller() {
        java.util.concurrent.CountDownLatch hang = new java.util.concurrent.CountDownLatch(1);
        doAnswer(inv -> {
            hang.await();
            return null;
        }).when(rabbit).convertAndSend(anyString(), anyString(), any(Object.class), any(MessagePostProcessor.class),
                any(CorrelationData.class));
        try {
            long start = System.nanoTime();
            publisher.publishJob(otp);
            publisher.publishJob(otp);
            assertThat(java.time.Duration.ofNanos(System.nanoTime() - start)).isLessThan(java.time.Duration.ofMillis(50));
        } finally {
            hang.countDown();
        }
    }

    @Test
    void retryCopyCarriesOriginalRoutingKeyAsCcAndPriority() {
        AtomicReference<MessageProperties> props = new AtomicReference<>();
        doAnswer(inv -> {
            MessagePostProcessor mpp = inv.getArgument(3);
            Message msg = mpp.postProcessMessage(new Message(new byte[0], new MessageProperties()));
            props.set(msg.getMessageProperties());
            CorrelationData correlation = inv.getArgument(4);
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbit).convertAndSend(eq("jobs.retry"), eq("jobs.retry.30s"), any(Object.class),
                any(MessagePostProcessor.class), any(CorrelationData.class));

        assertThat(publisher.publishRetry(otp, "jobs.retry.30s")).isTrue();
        assertThat(props.get().getHeaders().get("CC")).isEqualTo(List.of(JobTypes.OTP_DELIVERY));
        assertThat(props.get().getPriority()).isEqualTo(9);
    }
}
