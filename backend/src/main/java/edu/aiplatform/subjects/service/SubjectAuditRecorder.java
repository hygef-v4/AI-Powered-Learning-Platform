package edu.aiplatform.subjects.service;

import edu.aiplatform.audit.contract.AuditActions;
import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.ResourceRef;
import edu.aiplatform.subjects.model.Subject;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Success hook B08; IAudit thật phải tham gia transaction, exception không được nuốt. */
@Component
@Transactional(propagation = Propagation.MANDATORY)
public class SubjectAuditRecorder {
    private final IAudit audit;

    public SubjectAuditRecorder(IAudit audit) {
        this.audit = audit;
    }

    /**
     * Không tự gọi qua this: helper nhận Spring bean này để interceptor tạm dừng caller transaction.
     * U02 vẫn phải ghi DENIED bằng transaction riêng theo IAudit, kể cả caller không có transaction.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void recordAccessDenied(
            Actor actor, String attemptedAction, ResourceRef resource, String reason, Instant at) {
        audit.record(
                new AuditEvent(
                        UUID.randomUUID(),
                        actor == null ? null : actor.accountId(),
                        "ACCESS_DENIED",
                        "SUBJECT",
                        resource.id() == null ? null : resource.id().toString(),
                        AuditResult.DENIED,
                        reason,
                        Map.of(
                                "attemptedAction",
                                attemptedAction,
                                "scope",
                                resource.scope().name()),
                        at));
    }

    /** Chỉ ghi tạo, lưu trữ, mở lại môn; sửa thông tin hay đổi chủ nhiệm không ghi (yêu cầu 2026-11-26). */
    public void recordCreated(Actor actor, Subject subject) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("code", subject.code().value());
        details.put("managerId", subject.managerId());
        recordEvent(actor, "CREATED", subject, details, subject.createdAt());
    }

    public void recordStatusChanged(Actor actor, Subject before, Subject after) {
        recordEvent(
                actor,
                after.status() == edu.aiplatform.subjects.contract.SubjectStatus.ARCHIVED
                        ? "ARCHIVED"
                        : "REOPENED",
                after,
                Map.of("before", before.status().name(), "after", after.status().name()),
                after.updatedAt());
    }

    private void recordEvent(
            Actor actor, String event, Subject subject, Map<String, Object> details, Instant at) {
        audit.record(
                new AuditEvent(
                        UUID.randomUUID(),
                        actor.accountId(),
                        AuditActions.buildObjectAction("SUBJECT", subject.code().value(), event),
                        "SUBJECT",
                        subject.id().toString(),
                        AuditResult.SUCCESS,
                        null,
                        java.util.Collections.unmodifiableMap(details),
                        at));
    }
}
