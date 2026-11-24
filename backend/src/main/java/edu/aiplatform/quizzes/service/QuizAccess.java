package edu.aiplatform.quizzes.service;

import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.AuthorizationDecision;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.ResourceRef;
import edu.aiplatform.shared.error.ApiException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Kiểm quyền phía server cho U10 (BR-U10-50, 52): ngoài phạm vi trả {@code 404} (không ghi audit từ 2026-11-25);
 * U01/U04 không trả lời thì từ chối bằng {@code 503}, không ghi từ chối oan.
 */
@Component
class QuizAccess {

    /** Lý do U01 trả khi không kiểm được phạm vi (AuthorizationService.DEPENDENCY_UNAVAILABLE). */
    private static final String DEPENDENCY_UNAVAILABLE = "DEPENDENCY_UNAVAILABLE";

    private final IAuthorization authorization;

    QuizAccess(IAuthorization authorization) {
        this.authorization = authorization;
    }

    /** Kiểm {@code action} trên môn; không được thì 404 {@code notFoundCode}. */
    void requireSubject(Actor actor, String action, UUID subjectId, String notFoundCode) {
        require(actor, action, ResourceRef.subject(subjectId), notFoundCode);
    }

    /** Kiểm {@code action} trên lớp (giảng viên của lớp, R3/R4). */
    void requireClass(Actor actor, String action, UUID classId, String notFoundCode) {
        require(actor, action, ResourceRef.courseClass(classId), notFoundCode);
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
            throw createUnavailableError();
        }
        if (decision == null || (!decision.allowed() && DEPENDENCY_UNAVAILABLE.equals(decision.reason()))) {
            throw createUnavailableError();
        }
        return decision.allowed();
    }

    private void require(Actor actor, String action, ResourceRef resource, String notFoundCode) {
        if (actor == null || actor.accountId() == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "Cần đăng nhập");
        }
        AuthorizationDecision decision;
        try {
            decision = authorization.authorize(actor, action, resource);
        } catch (RuntimeException unavailable) {
            throw createUnavailableError();
        }
        if (decision == null || (!decision.allowed() && DEPENDENCY_UNAVAILABLE.equals(decision.reason()))) {
            throw createUnavailableError();
        }
        if (!decision.allowed()) {
            throw ApiException.createNotFound(notFoundCode);
        }
    }

    private static ApiException createUnavailableError() {
        return ApiException.createUnavailable("QUIZ_AUTHORIZATION_UNAVAILABLE", "Chưa kiểm tra được quyền, vui lòng thử lại");
    }
}
