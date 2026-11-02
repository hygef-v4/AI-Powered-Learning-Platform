package edu.aiplatform.assignmentwork.application;

import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.academics.contract.IClassAccess;
import edu.aiplatform.assignments.contract.AssignmentScope;
import edu.aiplatform.assignments.contract.AssignmentStatus;
import edu.aiplatform.assignments.contract.AssignmentView;
import edu.aiplatform.assignments.contract.IAssignmentQuery;
import edu.aiplatform.assignmentwork.contract.AttemptStatus;
import edu.aiplatform.assignmentwork.domain.AssignmentAttempt;
import edu.aiplatform.assignmentwork.domain.AttemptRepository;
import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.AuthorizationDecision;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.ResourceRef;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.shared.error.ApiException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Quyền U08 (BR-U08-01, 32, 60): chỉ Student đang ghi danh ACTIVE lớp OPEN; lượt chỉ chủ lượt thấy, giảng viên chính
 * của lớp ghi trên lượt được xem bài đã nộp. Ngoài phạm vi trả 404 kèm audit; U01, U05, U07 lỗi thì từ chối.
 */
@Component
class WorkAccess {

    private final IAuthorization authorization;
    private final IClassAccess classes;
    private final IAssignmentQuery assignments;
    private final AttemptRepository attempts;
    private final WorkAudit audit;

    WorkAccess(IAuthorization authorization, IClassAccess classes, IAssignmentQuery assignments,
               AttemptRepository attempts, WorkAudit audit) {
        this.authorization = authorization;
        this.classes = classes;
        this.assignments = assignments;
        this.attempts = attempts;
        this.audit = audit;
    }

    /** Người học đang học lớp OPEN (R5). */
    ClassRef requireStudentOfClass(Actor actor, UUID classId, String action) {
        requireSignedIn(actor);
        if (classId == null) {
            throw ApiException.badRequest("CLASS_REQUIRED", "Thiếu lớp");
        }
        if (actor.role() != Role.STUDENT || !allowed(actor, Actions.ASSIGNMENT_WORK, ResourceRef.courseClass(classId))) {
            throw deny(actor, action, "COURSE_CLASS", classId, "NOT_ENROLLED");
        }
        ClassRef ref = call(() -> classes.getClassRef(classId)).orElse(null);
        if (ref == null || ref.status() != ClassStatus.OPEN) {
            throw deny(actor, action, "COURSE_CLASS", classId, "CLASS_NOT_OPEN");
        }
        return ref;
    }

    /** Bài thuộc lớp hoặc môn chứa lớp; người học thấy OPEN, CLOSED, RETIRED khi đã có lượt nộp (BR-U07-37). */
    AssignmentView requireVisible(Actor actor, ClassRef ref, UUID assignmentId, String action) {
        AssignmentView view = call(() -> assignments.get(assignmentId)).orElse(null);
        if (view == null || !belongs(view, ref) || !visible(view, actor.accountId())) {
            throw deny(actor, action, WorkAudit.OBJECT_ASSIGNMENT, assignmentId, "ASSIGNMENT_NOT_VISIBLE");
        }
        return view;
    }

    static boolean belongs(AssignmentView view, ClassRef ref) {
        return view.scope() == AssignmentScope.CLASS ? ref.classId().equals(view.classId())
                : ref.subjectId().equals(view.subjectId());
    }

    boolean visible(AssignmentView view, UUID accountId) {
        return switch (view.status()) {
            case OPEN, CLOSED -> true;
            case RETIRED -> attempts.listMine(view.assignmentId(), accountId).stream()
                    .anyMatch(a -> a.status() == AttemptStatus.SUBMITTED);
            default -> false;
        };
    }

    /** Chủ lượt còn ghi danh lớp OPEN của lượt. */
    AssignmentAttempt requireOwnAttempt(Actor actor, UUID attemptId, String action) {
        requireSignedIn(actor);
        AssignmentAttempt attempt = attempts.find(attemptId).orElse(null);
        if (attempt == null || !attempt.accountId().equals(actor.accountId())) {
            throw deny(actor, action, WorkAudit.OBJECT_ATTEMPT, attemptId, "NOT_ATTEMPT_OWNER");
        }
        requireStudentOfClass(actor, attempt.classId(), action);
        return attempt;
    }

    AssignmentAttempt requireOwnAttemptForUpdate(Actor actor, UUID attemptId, String action) {
        AssignmentAttempt attempt = requireOwnAttempt(actor, attemptId, action);
        return attempts.findForUpdate(attempt.id()).orElseThrow(() -> ApiException.notFound("ATTEMPT_NOT_FOUND"));
    }

    /** Chủ lượt, hoặc giảng viên chính của lớp ghi trên lượt với lượt đã nộp (F5 xuất, F10 bước 4). */
    AssignmentAttempt requireReadableAttempt(Actor actor, UUID attemptId, String action) {
        requireSignedIn(actor);
        AssignmentAttempt attempt = attempts.find(attemptId).orElse(null);
        if (attempt != null && attempt.accountId().equals(actor.accountId())) {
            requireStudentOfClass(actor, attempt.classId(), action);
            return attempt;
        }
        if (attempt != null && actor.role() != Role.STUDENT && attempt.status() == AttemptStatus.SUBMITTED
                && allowed(actor, Actions.ASSIGNMENT_ATTEMPT_REVIEW, ResourceRef.courseClass(attempt.classId()))) {
            return attempt;
        }
        throw deny(actor, action, WorkAudit.OBJECT_ATTEMPT, attemptId, "OUT_OF_ATTEMPT_SCOPE");
    }

    boolean isActiveStudent(UUID accountId, UUID classId) {
        return call(() -> classes.isActiveStudent(accountId, classId));
    }

    ApiException deny(Actor actor, String action, String objectType, UUID objectId, String reason) {
        try {
            audit.denied(actor, action, objectType, objectId, reason);
        } catch (RuntimeException e) {
            return ApiException.unavailable("AUDIT_UNAVAILABLE", "Chưa ghi nhận được yêu cầu, vui lòng thử lại");
        }
        return ApiException.notFound("NOT_FOUND");
    }

    private boolean allowed(Actor actor, String action, ResourceRef resource) {
        AuthorizationDecision decision = call(() -> authorization.authorize(actor, action, resource));
        if (decision == null) {
            throw unavailable();
        }
        return decision.allowed();
    }

    static <T> T call(java.util.function.Supplier<T> work) {
        try {
            return work.get();
        } catch (ApiException e) {
            throw e;
        } catch (RuntimeException e) {
            throw unavailable();
        }
    }

    static boolean openForStudents(AssignmentView view) {
        return view.status() == AssignmentStatus.OPEN;
    }

    private static void requireSignedIn(Actor actor) {
        if (actor == null || actor.accountId() == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "Cần đăng nhập");
        }
    }

    private static ApiException unavailable() {
        return ApiException.unavailable("WORK_DEPENDENCY_UNAVAILABLE", "Chưa kiểm tra được quyền, vui lòng thử lại");
    }
}
