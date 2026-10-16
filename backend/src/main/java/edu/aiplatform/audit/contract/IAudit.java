package edu.aiplatform.audit.contract;

/**
 * Ghi audit cho mọi unit. {@code SUCCESS} ghi trong transaction của unit gọi; {@code DENIED} và
 * {@code FAILURE} ghi bằng transaction riêng để còn lại khi nghiệp vụ rollback (BR-U02-02).
 */
public interface IAudit {

    void record(AuditEvent event);
}
