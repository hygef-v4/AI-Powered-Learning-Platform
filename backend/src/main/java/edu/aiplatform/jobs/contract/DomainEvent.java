package edu.aiplatform.jobs.contract;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record DomainEvent(int schemaVersion, UUID eventId, String eventType, String sourceUnit, Instant occurredAt,
                          String correlationId, Map<String, Object> payload) {
}
