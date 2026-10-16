package edu.aiplatform.jobs.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import edu.aiplatform.jobs.contract.DomainEvent;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EventPublisherTest {

    final IMessageSender sender = mock(IMessageSender.class);
    final EventPublisher publisher = new EventPublisher(sender, new AfterCommitExecutor(Runnable::run));

    static DomainEvent event(Map<String, Object> payload) {
        return new DomainEvent(1, UUID.randomUUID(), "backup.failed", "U03", Instant.now(), null, payload);
    }

    @Test
    void lostEventsAreAcceptedSilently() {
        doThrow(new RuntimeException("broker down")).when(sender).sendEvent(any());

        publisher.publish(event(Map.of("runId", "r")));

        verify(sender).sendEvent(any());
    }

    @Test
    void forbiddenKeysAreRejected() {
        assertThatThrownBy(() -> publisher.publish(event(Map.of("token", "x"))))
                .isInstanceOf(IllegalArgumentException.class);
        verify(sender, never()).sendEvent(any());
    }
}
