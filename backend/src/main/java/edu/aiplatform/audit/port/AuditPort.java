package edu.aiplatform.audit.port;

/** U02 cung cấp cho mọi unit; ghi trong transaction của unit gọi (BR-U02-02). */
public interface AuditPort {
    void record(AuditEvent event);
    /** Ghi bằng transaction riêng để còn lại khi nghiệp vụ rollback. */
    void recordDenied(AuditEvent event);
    void recordFailure(AuditEvent event);
}
