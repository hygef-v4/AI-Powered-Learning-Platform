package edu.aiplatform.grading.service;

import edu.aiplatform.academics.contract.IClassAccess;
import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.AuthorizationDecision;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.ResourceRef;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class GradingAccess {
    private final IAuthorization auth;
    private final IClassAccess classes;
    private final IAudit audit;
    private final Clock clock;

    public GradingAccess(IAuthorization auth, IClassAccess classes, IAudit audit, Clock clock) {
        this.auth = auth;
        this.classes = classes;
        this.audit = audit;
        this.clock = clock;
    }

    public void requireTeacher(Actor actor, UUID classId, String action) {
        require(actor, classId, action);
    }

    public void requireStudent(Actor actor, UUID classId) {
        require(actor, classId, Actions.MY_GRADE_READ);
        final boolean enrolled;
        try {
            enrolled = classes.isActiveStudent(actor.accountId(), classId);
        } catch (RuntimeException ex) {
            throw ApiException.unavailable("GRADING_DEPENDENCY_UNAVAILABLE", "Enrollment service unavailable");
        }
        if (!enrolled) {
            audit.record(new AuditEvent(UUID.randomUUID(), actor.accountId(), "ACCESS_DENIED", "CLASS",
                    String.valueOf(classId), AuditResult.DENIED, "ENROLLMENT_REQUIRED",
                    Map.of("action", Actions.MY_GRADE_READ), clock.instant()));
            throw ApiException.notFound("GRADE_NOT_FOUND");
        }
    }

    private void require(Actor actor, UUID classId, String action) {
        final AuthorizationDecision decision;
        try {
            decision = auth.authorize(actor, action, ResourceRef.courseClass(classId));
        } catch (RuntimeException ex) {
            throw ApiException.unavailable("GRADING_DEPENDENCY_UNAVAILABLE", "Access service unavailable");
        }
        if (decision == null || "DEPENDENCY_UNAVAILABLE".equals(decision.reason()))
            throw ApiException.unavailable("GRADING_DEPENDENCY_UNAVAILABLE", "Access service unavailable");
        if (!decision.allowed()) {
            audit.record(new AuditEvent(UUID.randomUUID(), actor == null ? null : actor.accountId(), "ACCESS_DENIED",
                    "CLASS", String.valueOf(classId), AuditResult.DENIED, "GRADING_SCOPE", Map.of("action", action),
                    clock.instant()));
            throw ApiException.notFound("GRADE_NOT_FOUND");
        }
    }
}
