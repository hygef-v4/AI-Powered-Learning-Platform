package edu.aiplatform.quizpractice.service;

import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.AuthorizationDecision;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.ResourceRef;
import edu.aiplatform.shared.error.ApiException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Kiểm quyền của U11 (BR-U11-41, 40): ngoài phạm vi trả {@code 404} (không ghi audit từ 2026-11-25); U01 không
 * trả lời thì {@code 503}, không ghi từ chối oan. Không ghi nội dung câu trả lời (NFR-U11-31).
 */
@Component
class PracticeAccess {

    static final String OBJECT = "QUIZ_ATTEMPT";
    /** Lý do U01 trả khi không kiểm được phạm vi (AuthorizationService.DEPENDENCY_UNAVAILABLE). */
    private static final String DEPENDENCY_UNAVAILABLE = "DEPENDENCY_UNAVAILABLE";

    private final IAuthorization authorization;

    PracticeAccess(IAuthorization authorization) {
        this.authorization = authorization;
    }

    /** Kiểm {@code action} trên lớp: sinh viên đang học (R5) hoặc giảng viên của lớp (R3/R4). */
    void requireClass(Actor actor, String action, UUID classId, String notFoundCode) {
        if (actor == null || actor.accountId() == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "Cần đăng nhập");
        }
        if (!isClassAllowed(actor, action, classId)) {
            notFound(notFoundCode);
        }
    }

    /** Current permission for an available action; hiding an action does not deny access to an owned result. */
    boolean isClassAllowed(Actor actor, String action, UUID classId) {
        if (actor == null || actor.accountId() == null) {
            return false;
        }
        AuthorizationDecision decision;
        try {
            decision = authorization.authorize(actor, action, ResourceRef.courseClass(classId));
        } catch (RuntimeException unavailable) {
            throw createUnavailableError();
        }
        if (decision == null || (!decision.allowed() && DEPENDENCY_UNAVAILABLE.equals(decision.reason()))) {
            throw createUnavailableError();
        }
        return decision.allowed();
    }

    /** Từ chối bằng 404 (ví dụ xem lượt của người khác, BR-U11-22). */
    ApiException notFound(String notFoundCode) {
        throw ApiException.createNotFound(notFoundCode);
    }

    private static ApiException createUnavailableError() {
        return ApiException.createUnavailable("QUIZ_PRACTICE_AUTHORIZATION_UNAVAILABLE", "Chưa kiểm tra được quyền, vui lòng thử lại");
    }
}
