package edu.aiplatform.jobs.port;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Chỉ dùng cho thông báo U16; gửi sau commit, mất thì chấp nhận (BR-U03-70). */
public record DomainEvent(int schemaVersion, UUID eventId, String eventType, String sourceUnit, Instant occurredAt,
                          String correlationId, Map<String, Object> payload) {}
