package edu.aiplatform.audit.contract;

/**
 * {@code actorEmail} lấy lúc đọc; rỗng nghĩa là hệ thống. {@code objectLabel} là tên dễ đọc của đối tượng lúc đọc
 * (email tài khoản, mã và tên môn hoặc lớp); rỗng khi loại đối tượng không có tên hoặc không tra được.
 */
public record AuditLogView(AuditEvent event, String actorEmail, String objectLabel) {

    public AuditLogView(AuditEvent event, String actorEmail) {
        this(event, actorEmail, null);
    }
}
