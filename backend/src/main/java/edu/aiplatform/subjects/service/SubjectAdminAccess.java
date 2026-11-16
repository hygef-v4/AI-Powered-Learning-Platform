package edu.aiplatform.subjects.service;

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
    private final SubjectAuditRecorder subjectAuditRecorder;
    private final Clock clock;

    SubjectAdminAccess(
            IAuthorization authorization, SubjectAuditRecorder subjectAuditRecorder, Clock clock) {
        this.authorization = authorization;
        this.subjectAuditRecorder = subjectAuditRecorder;
        this.clock = clock;
    }

    void requireAccess(Actor actor, String action, ResourceRef resource) {
        if (actor == null || actor.accountId() == null) {
            throw rejectAccess(
                    actor,
                    action,
                    resource,
                    new ApiException(
                            HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "Cần đăng nhập"));
        }
        if (actor.role() != Role.ADMIN) {
            throw rejectAccess(
                    actor, action, resource, ApiException.createForbidden("SUBJECT_FORBIDDEN"));
        }
        AuthorizationDecision decision;
        try {
            decision = authorization.authorize(actor, action, resource);
        } catch (RuntimeException unavailable) {
            throw rejectAccess(actor, action, resource, createAuthorizationUnavailable());
        }
        if (decision == null) {
            throw rejectAccess(actor, action, resource, createAuthorizationUnavailable());
        }
        if (!decision.allowed()) {
            throw rejectAccess(
                    actor, action, resource, ApiException.createForbidden("SUBJECT_FORBIDDEN"));
        }
    }

    private ApiException createAuthorizationUnavailable() {
        return ApiException.createUnavailable(
                "SUBJECT_AUTHORIZATION_UNAVAILABLE", "Chưa kiểm tra được quyền, vui lòng thử lại");
    }

    private ApiException rejectAccess(
            Actor actor, String action, ResourceRef resource, ApiException rejection) {
        try {
            subjectAuditRecorder.recordAccessDenied(
                    actor, action, resource, rejection.readCode(), clock.instant());
        } catch (RuntimeException unavailable) {
            throw ApiException.createUnavailable(
                    "SUBJECT_AUDIT_UNAVAILABLE", "Chưa ghi nhận được yêu cầu, vui lòng thử lại");
        }
        return rejection;
    }
}
