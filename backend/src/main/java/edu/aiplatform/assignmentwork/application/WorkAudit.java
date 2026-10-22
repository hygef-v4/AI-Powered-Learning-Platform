package edu.aiplatform.assignmentwork.application;

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
 * Audit của U08 (BR-U08-61): {@code ASSIGNMENT_SUBMITTED}, {@code SUBMIT_REJECTED}, {@code PRACTICE_FEEDBACK_REQUESTED},
 * {@code ACCESS_DENIED}. Không audit từng lần lưu nháp và từng sự kiện hoạt động; không ghi nội dung bài làm.
 */
@Component
class WorkAudit {

    static final String OBJECT_ATTEMPT = "ASSIGNMENT_ATTEMPT";
    static final String OBJECT_ASSIGNMENT = "ASSIGNMENT";

    private final IAudit audit;
    private final Clock clock;

    WorkAudit(IAudit audit, Clock clock) {
        this.audit = audit;
        this.clock = clock;
    }

    void success(UUID actorId, String action, String objectType, UUID objectId, Map<String, Object> details) {
        audit.record(new AuditEvent(UUID.randomUUID(), actorId, action, objectType, objectId.toString(),
                AuditResult.SUCCESS, null, clean(details), clock.instant()));
    }

    /** Từ chối nghiệp vụ (quá hạn, hết lượt): U02 ghi bằng transaction riêng nên còn lại khi thao tác rollback. */
    void rejected(Actor actor, String action, String objectType, UUID objectId, String reason, Map<String, Object> details) {
        audit.record(new AuditEvent(UUID.randomUUID(), actor == null ? null : actor.accountId(), action, objectType,
                objectId == null ? null : objectId.toString(), AuditResult.FAILURE, reason, clean(details),
                clock.instant()));
    }

    void denied(Actor actor, String attemptedAction, String objectType, UUID objectId, String reason) {
        audit.record(new AuditEvent(UUID.randomUUID(), actor == null ? null : actor.accountId(), "ACCESS_DENIED",
                objectType, objectId == null ? null : objectId.toString(), AuditResult.DENIED, reason,
                Map.of("attemptedAction", attemptedAction), clock.instant()));
    }

    static Map<String, Object> details(Object... keyValues) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            out.put((String) keyValues[i], keyValues[i + 1]);
        }
        return out;
    }

    private static Map<String, Object> clean(Map<String, Object> details) {
        Map<String, Object> out = new LinkedHashMap<>();
        details.forEach((key, value) -> {
            if (value != null) {
                out.put(key, value instanceof UUID || value instanceof Enum<?> || value instanceof java.time.Instant
                        ? value.toString() : value);
            }
        });
        return Collections.unmodifiableMap(out);
    }
}
