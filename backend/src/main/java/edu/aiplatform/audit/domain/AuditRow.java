package edu.aiplatform.audit.domain;

import java.time.Instant;
import java.util.UUID;

/** Một dòng {@code audit_logs}; {@code detailsJson} là JSON đã che, đọc nguyên văn từ cột {@code details}. */
public record AuditRow(UUID id, UUID actorId, String action, String objectType, String objectId, String result,
                       String reason, String detailsJson, Instant occurredAt) {
}
