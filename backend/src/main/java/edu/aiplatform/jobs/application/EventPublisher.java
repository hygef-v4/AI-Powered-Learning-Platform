package edu.aiplatform.jobs.application;

import edu.aiplatform.jobs.infrastructure.AmqpPublisher;
import edu.aiplatform.jobs.port.DomainEvent;
import edu.aiplatform.jobs.port.EventPublisherPort;
import edu.aiplatform.shared.security.ForbiddenKeyGuard;
import org.springframework.stereotype.Service;

/**
 * Phát event thông báo (E1): gửi sau commit, không đảm bảo giao hàng, lỗi chỉ log (BR-U03-70, 71).
 * Chỉ dùng cho thông báo U16; phản ứng bắt buộc giữa unit đi qua port (BR-U03-64).
 */
@Service
public class EventPublisher implements EventPublisherPort {

    public static final int SCHEMA_VERSION = 1;

    private final AmqpPublisher amqp;

    public EventPublisher(AmqpPublisher amqp) {
        this.amqp = amqp;
    }

    @Override
    public void publish(DomainEvent event) {
        if (event.schemaVersion() != SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported schemaVersion: " + event.schemaVersion());
        }
        if (event.eventType() == null || event.eventType().isBlank()) {
            throw new IllegalArgumentException("eventType is required");
        }
        ForbiddenKeyGuard.check(event.payload());
        DomainEvent toSend = event.correlationId() != null ? event
                : new DomainEvent(event.schemaVersion(), event.eventId(), event.eventType(), event.sourceUnit(),
                        event.occurredAt(), AfterCommit.correlationId(), event.payload());
        AfterCommit.run(() -> amqp.publishEvent(toSend));
    }
}
