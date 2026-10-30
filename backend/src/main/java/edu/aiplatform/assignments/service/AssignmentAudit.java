package edu.aiplatform.assignments.service;

import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.identity.contract.Actor;
import java.time.Clock;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Audit của U07 (BR-U07-71): {@code SUCCESS} ghi trong transaction của thao tác; {@code ACCESS_DENIED} do U02 ghi
 * bằng transaction riêng. Không ghi nội dung đề, hướng dẫn, rubric, prompt (NFR-U07-33).
 */
@Component
class AssignmentAudit {

    static final String OBJECT_ASSIGNMENT = "ASSIGNMENT";

    private final IAudit audit;
    private final Clock clock;

    AssignmentAudit(IAudit audit, Clock clock) {
        this.audit = audit;
        this.clock = clock;
    }

    void success(Actor actor, String action, UUID assignmentId, Map<String, Object> details) {
        audit.record(new AuditEvent(UUID.randomUUID(), actor == null ? null : actor.accountId(), action,
                OBJECT_ASSIGNMENT, assignmentId.toString(), AuditResult.SUCCESS, null, clean(details), clock.instant()));
    }

    void denied(Actor actor, String attemptedAction, String objectType, UUID objectId, String reason) {
        audit.record(new AuditEvent(UUID.randomUUID(), actor == null ? null : actor.accountId(), "ACCESS_DENIED",
                objectType, objectId == null ? null : objectId.toString(), AuditResult.DENIED, reason,
                Map.of("attemptedAction", attemptedAction), clock.instant()));
    }

    /** Bỏ giá trị null (Map.of không nhận null), giữ thứ tự để đọc nhật ký dễ. */
    static Map<String, Object> clean(Map<String, Object> details) {
        Map<String, Object> out = new LinkedHashMap<>();
        details.forEach((key, value) -> {
            if (value != null) {
                out.put(key, value instanceof UUID || value instanceof Enum<?> || value instanceof java.time.Instant
                        ? value.toString() : value);
            }
        });
        return Collections.unmodifiableMap(out);
    }

    static Map<String, Object> details(Object... keyValues) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            out.put((String) keyValues[i], keyValues[i + 1]);
        }
        return out;
    }
}
