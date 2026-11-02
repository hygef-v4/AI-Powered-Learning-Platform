package edu.aiplatform.quizpractice.service;

import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.AuthorizationDecision;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.ResourceRef;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Kiểm quyền và audit của U11 (BR-U11-41, 40): ngoài phạm vi trả {@code 404} và ghi {@code ACCESS_DENIED}; U01 không
 * trả lời thì {@code 503}, không ghi từ chối oan. Không ghi nội dung câu trả lời (NFR-U11-31).
 */
@Component
class PracticeAccess {

    static final String OBJECT = "QUIZ_ATTEMPT";
    /** Lý do U01 trả khi không kiểm được phạm vi (AuthorizationService.DEPENDENCY_UNAVAILABLE). */
    private static final String DEPENDENCY_UNAVAILABLE = "DEPENDENCY_UNAVAILABLE";

    private final IAuthorization authorization;
    private final IAudit audit;
    private final Clock clock;

    PracticeAccess(IAuthorization authorization, IAudit audit, Clock clock) {
        this.authorization = authorization;
        this.audit = audit;
        this.clock = clock;
    }

    /** Kiểm {@code action} trên lớp: sinh viên đang học (R5) hoặc giảng viên của lớp (R3/R4). */
    void requireClass(Actor actor, String action, UUID classId, String objectType, UUID objectId, String notFoundCode) {
        if (actor == null || actor.accountId() == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "Cần đăng nhập");
        }
        if (!allowedClass(actor, action, classId)) {
            deny(actor, action, objectType, objectId, notFoundCode);
        }
    }

    /** Current permission for an available action; hiding an action does not deny access to an owned result. */
    boolean allowedClass(Actor actor, String action, UUID classId) {
        if (actor == null || actor.accountId() == null) {
            return false;
        }
        AuthorizationDecision decision;
        try {
            decision = authorization.authorize(actor, action, ResourceRef.courseClass(classId));
        } catch (RuntimeException unavailable) {
            throw unavailable();
        }
        if (decision == null || (!decision.allowed() && DEPENDENCY_UNAVAILABLE.equals(decision.reason()))) {
            throw unavailable();
        }
        return decision.allowed();
    }

    /** Từ chối kèm audit {@code ACCESS_DENIED} (ví dụ xem lượt của người khác, BR-U11-22). */
    ApiException deny(Actor actor, String action, String objectType, UUID objectId, String notFoundCode) {
        try {
            record(actor, "ACCESS_DENIED", objectType, objectId, AuditResult.DENIED, notFoundCode, Map.of("action", action));
        } catch (RuntimeException auditDown) {
            throw ApiException.unavailable("QUIZ_PRACTICE_AUDIT_UNAVAILABLE", "Chưa ghi nhận được yêu cầu, vui lòng thử lại");
        }
        throw ApiException.notFound(notFoundCode);
    }

    void success(Actor actor, String action, UUID attemptId, Map<String, Object> details) {
        record(actor, action, OBJECT, attemptId, AuditResult.SUCCESS, null, details);
    }

    void rejected(Actor actor, String action, UUID attemptId, String reason) {
        record(actor, action, OBJECT, attemptId, AuditResult.FAILURE, reason, Map.of());
    }

    private void record(Actor actor, String action, String objectType, UUID objectId, AuditResult result, String reason,
                        Map<String, Object> details) {
        audit.record(new AuditEvent(UUID.randomUUID(), actor == null ? null : actor.accountId(), action, objectType,
                objectId == null ? null : objectId.toString(), result, reason, details, clock.instant()));
    }

    private static ApiException unavailable() {
        return ApiException.unavailable("QUIZ_PRACTICE_AUTHORIZATION_UNAVAILABLE", "Chưa kiểm tra được quyền, vui lòng thử lại");
    }
}
