package edu.aiplatform.jobs.port;

import java.util.Map;

/** Payload chỉ chứa ID/tham chiếu (BR-U03-52). */
public record JobMessage(int schemaVersion, String jobType, String idempotencyKey, Map<String, Object> payload,
                         int attempt, String correlationId) {}
