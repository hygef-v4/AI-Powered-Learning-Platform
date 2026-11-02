package edu.aiplatform.quizzes.application;

import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.AuthorizationDecision;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.ResourceRef;
import edu.aiplatform.shared.error.ApiException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Kiểm quyền phía server cho U10 (BR-U10-50, 52): ngoài phạm vi trả {@code 404} và ghi audit {@code ACCESS_DENIED};
 * U01/U04 không trả lời thì từ chối bằng {@code 503}, không ghi từ chối oan.
 */
@Component
class QuizAccess {

    /** Lý do U01 trả khi không kiểm được phạm vi (AuthorizationService.DEPENDENCY_UNAVAILABLE). */
    private static final String DEPENDENCY_UNAVAILABLE = "DEPENDENCY_UNAVAILABLE";

    private final IAuthorization authorization;
    private final QuizAudit audit;

    QuizAccess(IAuthorization authorization, QuizAudit audit) {
        this.authorization = authorization;
        this.audit = audit;
    }

    /** Kiểm {@code action} trên môn; {@code objectType}, {@code objectId} là đối tượng ghi vào audit khi từ chối. */
    void requireSubject(Actor actor, String action, UUID subjectId, String objectType, UUID objectId,
                        String notFoundCode) {
        require(actor, action, ResourceRef.subject(subjectId), objectType, objectId, notFoundCode);
    }

    /** Kiểm {@code action} trên lớp (giảng viên của lớp, R3/R4). */
    void requireClass(Actor actor, String action, UUID classId, String objectType, UUID objectId,
                      String notFoundCode) {
        require(actor, action, ResourceRef.courseClass(classId), objectType, objectId, notFoundCode);
    }

    /** Hỏi quyền mà không ghi từ chối (để biết người xem có được sửa không); U01 lỗi vẫn trả {@code 503}. */
    boolean allows(Actor actor, String action, ResourceRef resource) {
        if (actor == null || actor.accountId() == null) {
            return false;
        }
        AuthorizationDecision decision;
        try {
            decision = authorization.authorize(actor, action, resource);
        } catch (RuntimeException unavailable) {
            throw unavailable();
        }
        if (decision == null || (!decision.allowed() && DEPENDENCY_UNAVAILABLE.equals(decision.reason()))) {
            throw unavailable();
        }
        return decision.allowed();
    }

    private void require(Actor actor, String action, ResourceRef resource, String objectType, UUID objectId,
                         String notFoundCode) {
        if (actor == null || actor.accountId() == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "Cần đăng nhập");
        }
        AuthorizationDecision decision;
        try {
            decision = authorization.authorize(actor, action, resource);
        } catch (RuntimeException unavailable) {
            throw unavailable();
        }
        if (decision == null || (!decision.allowed() && DEPENDENCY_UNAVAILABLE.equals(decision.reason()))) {
            throw unavailable();
        }
        if (!decision.allowed()) {
            try {
                audit.denied(actor, action, objectType, objectId, notFoundCode);
            } catch (RuntimeException auditDown) {
                throw ApiException.unavailable("QUIZ_AUDIT_UNAVAILABLE", "Chưa ghi nhận được yêu cầu, vui lòng thử lại");
            }
            throw ApiException.notFound(notFoundCode);
        }
    }

    private static ApiException unavailable() {
        return ApiException.unavailable("QUIZ_AUTHORIZATION_UNAVAILABLE", "Chưa kiểm tra được quyền, vui lòng thử lại");
    }
}
