package edu.aiplatform.jobs.contract;

import java.util.Map;

/** Payload chỉ chứa ID và tham chiếu; khóa nhạy cảm bị chặn bởi {@code ForbiddenKeyGuard}. */
public record JobMessage(int schemaVersion, String jobType, String idempotencyKey, Map<String, Object> payload,
                         int attempt, String correlationId) {
}
