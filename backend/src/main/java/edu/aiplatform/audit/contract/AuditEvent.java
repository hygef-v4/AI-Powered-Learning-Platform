package edu.aiplatform.audit.contract;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** {@code id} do unit gọi tạo (ghi lại cùng id không sinh bản ghi thứ hai); {@code details} đã che. */
public record AuditEvent(UUID id, UUID actorId, String action, String objectType, String objectId,
                         AuditResult result, String reason, Map<String, Object> details, Instant occurredAt) {
}
