package edu.aiplatform.jobs.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import edu.aiplatform.jobs.infrastructure.AmqpPublisher;
import edu.aiplatform.jobs.port.DomainEvent;
import edu.aiplatform.jobs.port.EventTypes;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

/** BR-U03-70, 71. */
class EventPublisherTest {

    private final AmqpPublisher amqp = mock(AmqpPublisher.class);
    private final EventPublisher publisher = new EventPublisher(amqp);

    @AfterEach
    void clearTransaction() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void publishesAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);

        publisher.publish(event(1, Map.of("classId", "c1", "accountId", "a1")));
        verify(amqp, never()).publishEvent(any());

        TransactionSynchronizationUtils.triggerAfterCommit();
        verify(amqp).publishEvent(any());
    }

    @Test
    void publishesImmediatelyWithoutTransaction() {
        publisher.publish(event(1, Map.of("classId", "c1")));
        verify(amqp).publishEvent(any());
    }

    @Test
    void rejectsUnsupportedSchemaVersionAndSecrets() {
        assertThatThrownBy(() -> publisher.publish(event(2, Map.of()))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> publisher.publish(event(1, Map.of("resetToken", "x"))))
                .isInstanceOf(IllegalArgumentException.class);
        verify(amqp, never()).publishEvent(any());
    }

    private static DomainEvent event(int schemaVersion, Map<String, Object> payload) {
        return new DomainEvent(schemaVersion, UUID.randomUUID(), EventTypes.ENROLLMENT_ACTIVATED, "U04", Instant.now(),
                null, payload);
    }
}
