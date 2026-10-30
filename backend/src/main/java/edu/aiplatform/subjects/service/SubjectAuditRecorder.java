package edu.aiplatform.subjects.service;

import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.ResourceRef;
import edu.aiplatform.subjects.model.Subject;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
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
    public void accessDenied(Actor actor, String attemptedAction, ResourceRef resource, String reason, Instant at) {
        audit.record(new AuditEvent(UUID.randomUUID(), actor == null ? null : actor.accountId(), "ACCESS_DENIED", "SUBJECT",
                resource.id() == null ? null : resource.id().toString(), AuditResult.DENIED, reason,
                Map.of("attemptedAction", attemptedAction, "scope", resource.scope().name()), at));
    }

    public void created(Actor actor, Subject subject) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("code", subject.code().value());
        details.put("managerId", subject.managerId());
        record(actor, "SUBJECT_CREATED", subject, details, subject.createdAt());
    }

    public void informationUpdated(Actor actor, Subject before, Subject after) {
        List<String> fields = new java.util.ArrayList<>();
        if (!before.name().equals(after.name())) {
            fields.add("name");
        }
        if (!java.util.Objects.equals(before.description(), after.description())) {
            fields.add("description");
        }
        record(actor, "SUBJECT_UPDATED", after, Map.of("changedFields", List.copyOf(fields)), after.updatedAt());
    }

    public void managerChanged(Actor actor, Subject before, Subject after) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("oldManagerId", before.managerId());
        details.put("newManagerId", after.managerId());
        record(actor, "SUBJECT_MANAGER_CHANGED", after, details, after.updatedAt());
    }

    public void statusChanged(Actor actor, Subject before, Subject after) {
        record(actor, after.status() == edu.aiplatform.subjects.contract.SubjectStatus.ARCHIVED
                        ? "SUBJECT_ARCHIVED" : "SUBJECT_REOPENED", after,
                Map.of("before", before.status().name(), "after", after.status().name()), after.updatedAt());
    }

    private void record(Actor actor, String action, Subject subject, Map<String, Object> details, Instant at) {
        audit.record(new AuditEvent(UUID.randomUUID(), actor.accountId(), action, "SUBJECT", subject.id().toString(),
                AuditResult.SUCCESS, null, java.util.Collections.unmodifiableMap(details), at));
    }
}
