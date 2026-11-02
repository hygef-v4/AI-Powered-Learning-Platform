package edu.aiplatform.quizzes.application;

import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.identity.contract.Actor;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Audit của U10 (BR-U10-51). Chỉ ghi id, số đếm, tên trường; không ghi nội dung câu, đáp án (NFR-U10-32). */
@Component
class QuizAudit {

    private final IAudit audit;
    private final Clock clock;

    QuizAudit(IAudit audit, Clock clock) {
        this.audit = audit;
        this.clock = clock;
    }

    void success(Actor actor, String action, String objectType, UUID objectId, Map<String, Object> details) {
        record(actor, action, objectType, objectId, AuditResult.SUCCESS, null, details);
    }

    void denied(Actor actor, String action, String objectType, UUID objectId, String reason) {
        record(actor, "ACCESS_DENIED", objectType, objectId, AuditResult.DENIED, reason, Map.of("action", action));
    }

    private void record(Actor actor, String action, String objectType, UUID objectId, AuditResult result,
                        String reason, Map<String, Object> details) {
        audit.record(new AuditEvent(UUID.randomUUID(), actor == null ? null : actor.accountId(), action, objectType,
                objectId == null ? null : objectId.toString(), result, reason, details, clock.instant()));
    }
}
