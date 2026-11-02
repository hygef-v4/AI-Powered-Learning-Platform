package edu.aiplatform.subjects.application;

import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.AuthorizationDecision;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.ResourceRef;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import org.springframework.http.HttpStatus;

/** Dùng chung cho đọc/ghi U04, không thay session/current account checks của U01. */
final class SubjectAdminAccess {
    private final IAuthorization authorization;
    private final SubjectAuditRecorder audit;
    private final Clock clock;

    SubjectAdminAccess(IAuthorization authorization, SubjectAuditRecorder audit, Clock clock) {
        this.authorization = authorization;
        this.audit = audit;
        this.clock = clock;
    }

    void require(Actor actor, String action, ResourceRef resource) {
        if (actor == null || actor.accountId() == null) {
            throw reject(actor, action, resource, new ApiException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "Cần đăng nhập"));
        }
        if (actor.role() != Role.ADMIN) {
            throw reject(actor, action, resource, ApiException.forbidden("SUBJECT_FORBIDDEN"));
        }
        AuthorizationDecision decision;
        try {
            decision = authorization.authorize(actor, action, resource);
        } catch (RuntimeException unavailable) {
            throw reject(actor, action, resource, authorizationUnavailable());
        }
        if (decision == null) {
            throw reject(actor, action, resource, authorizationUnavailable());
        }
        if (!decision.allowed()) {
            throw reject(actor, action, resource, ApiException.forbidden("SUBJECT_FORBIDDEN"));
        }
    }

    private ApiException authorizationUnavailable() {
        return ApiException.unavailable("SUBJECT_AUTHORIZATION_UNAVAILABLE", "Chưa kiểm tra được quyền, vui lòng thử lại");
    }

    private ApiException reject(Actor actor, String action, ResourceRef resource, ApiException rejection) {
        try {
            audit.accessDenied(actor, action, resource, rejection.code(), clock.instant());
        } catch (RuntimeException unavailable) {
            throw ApiException.unavailable("SUBJECT_AUDIT_UNAVAILABLE", "Chưa ghi nhận được yêu cầu, vui lòng thử lại");
        }
        return rejection;
    }
}
