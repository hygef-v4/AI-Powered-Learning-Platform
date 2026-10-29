package edu.aiplatform.audit.contract;

/** {@code actorEmail} lấy lúc đọc; rỗng nghĩa là hệ thống. */
public record AuditLogView(AuditEvent event, String actorEmail) {
}
