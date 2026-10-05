package edu.aiplatform.audit.port;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** `id` do unit gọi tạo; `details` đã che dữ liệu nhạy cảm (BR-U02-03, 05). */
public record AuditEvent(UUID id, UUID actorId, String action, String objectType, String objectId,
                         AuditResult result, String reason, Map<String, Object> details, Instant occurredAt) {}
