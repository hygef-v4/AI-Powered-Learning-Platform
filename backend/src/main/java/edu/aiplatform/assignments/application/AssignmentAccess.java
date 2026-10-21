package edu.aiplatform.assignments.application;

import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.academics.contract.IClassAccess;
import edu.aiplatform.assignments.contract.AssignmentScope;
import edu.aiplatform.assignments.domain.Assignment;
import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.AuthorizationDecision;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.IClassScope;
import edu.aiplatform.identity.contract.ResourceRef;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.subjects.contract.ISubjectQuery;
import edu.aiplatform.subjects.contract.SubjectStatus;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Quyền U07 (BR-U07-02, 70): bài của lớp do giảng viên chính của lớp soạn ({@code ASSIGNMENT_CLASS_MANAGE}); bài
 * của môn do Chủ nhiệm môn soạn ({@code ASSIGNMENT_SUBJECT_MANAGE}); giảng viên các lớp của môn chỉ xem bài của môn
 * đã phát hành. Ngoài phạm vi trả 404 kèm audit {@code ACCESS_DENIED}; U01, U04, U05 lỗi thì từ chối (fail closed).
 */
@Component
class AssignmentAccess {

    private final IAuthorization authorization;
    private final IClassScope classScope;
    private final IClassAccess classes;
    private final ISubjectQuery subjects;
    private final AssignmentAudit audit;

    AssignmentAccess(IAuthorization authorization, IClassScope classScope, IClassAccess classes,
                     ISubjectQuery subjects, AssignmentAudit audit) {
        this.authorization = authorization;
        this.classScope = classScope;
        this.classes = classes;
        this.subjects = subjects;
        this.audit = audit;
    }

    /** Giảng viên chính của lớp; trả lớp để kiểm trạng thái. */
    ClassRef requireClassManager(Actor actor, UUID classId, String action) {
        requireSignedIn(actor);
        if (!allowed(actor, Actions.ASSIGNMENT_CLASS_MANAGE, ResourceRef.courseClass(classId))) {
            throw deny(actor, action, "COURSE_CLASS", classId, "OUT_OF_CLASS_SCOPE");
        }
        return classRef(classId);
    }

    void requireSubjectManager(Actor actor, UUID subjectId, String action) {
        requireSignedIn(actor);
        if (!allowed(actor, Actions.ASSIGNMENT_SUBJECT_MANAGE, ResourceRef.subject(subjectId))) {
            throw deny(actor, action, "SUBJECT", subjectId, "OUT_OF_SUBJECT_SCOPE");
        }
    }

    boolean canManage(Actor actor, Assignment assignment) {
        if (actor == null || actor.accountId() == null) {
            return false;
        }
        return assignment.scope() == AssignmentScope.CLASS
                ? allowed(actor, Actions.ASSIGNMENT_CLASS_MANAGE, ResourceRef.courseClass(assignment.classId()))
                : allowed(actor, Actions.ASSIGNMENT_SUBJECT_MANAGE, ResourceRef.subject(assignment.subjectId()));
    }

    void requireManage(Actor actor, Assignment assignment, String action) {
        requireSignedIn(actor);
        if (!canManage(actor, assignment)) {
            throw deny(actor, action, AssignmentAudit.OBJECT_ASSIGNMENT, assignment.id(), "NOT_ASSIGNMENT_AUTHOR");
        }
    }

    /** Người soạn, hoặc giảng viên một lớp của môn với bài của môn đã phát hành (chỉ xem). */
    boolean canRead(Actor actor, Assignment assignment) {
        if (canManage(actor, assignment)) {
            return true;
        }
        if (assignment.scope() != AssignmentScope.SUBJECT || !assignment.published()
                || actor.role() == Role.STUDENT || actor.role() == Role.ADMIN) {
            return false;
        }
        for (UUID classId : classScope.listTaughtClasses(actor.accountId())) {
            if (classScope.subjectOfClass(classId).map(assignment.subjectId()::equals).orElse(false)
                    && allowed(actor, Actions.ASSIGNMENT_CLASS_MANAGE, ResourceRef.courseClass(classId))) {
                return true;
            }
        }
        return false;
    }

    void requireRead(Actor actor, Assignment assignment, String action) {
        requireSignedIn(actor);
        boolean readable;
        try {
            readable = canRead(actor, assignment);
        } catch (ApiException e) {
            throw e;
        } catch (RuntimeException e) {
            throw unavailable();
        }
        if (!readable) {
            throw deny(actor, action, AssignmentAudit.OBJECT_ASSIGNMENT, assignment.id(), "OUT_OF_ASSIGNMENT_SCOPE");
        }
    }

    /** Lớp hoặc môn đã lưu trữ thì bài chỉ đọc (BR-U07-04). */
    boolean ownerArchived(Assignment assignment) {
        if (assignment.scope() == AssignmentScope.CLASS) {
            return classRef(assignment.classId()).status() == ClassStatus.ARCHIVED;
        }
        return subjectStatus(assignment.subjectId()) == SubjectStatus.ARCHIVED;
    }

    void requireOwnerWritable(Assignment assignment) {
        if (ownerArchived(assignment)) {
            throw ApiException.conflict("ASSIGNMENT_READ_ONLY", assignment.scope() == AssignmentScope.CLASS
                    ? "Lớp đã lưu trữ, bài chỉ xem được" : "Môn đã lưu trữ, bài chỉ xem được");
        }
    }

    ClassRef classRef(UUID classId) {
        try {
            return classes.getClassRef(classId).orElseThrow(() -> ApiException.notFound("CLASS_NOT_FOUND"));
        } catch (ApiException e) {
            throw e;
        } catch (RuntimeException e) {
            throw unavailable();
        }
    }

    SubjectStatus subjectStatus(UUID subjectId) {
        try {
            return subjects.getSubject(subjectId).orElseThrow(() -> ApiException.notFound("SUBJECT_NOT_FOUND"))
                    .status();
        } catch (ApiException e) {
            throw e;
        } catch (RuntimeException e) {
            throw unavailable();
        }
    }

    ApiException deny(Actor actor, String action, String objectType, UUID objectId, String reason) {
        try {
            audit.denied(actor, action, objectType, objectId, reason);
        } catch (RuntimeException e) {
            return ApiException.unavailable("AUDIT_UNAVAILABLE", "Chưa ghi nhận được yêu cầu, vui lòng thử lại");
        }
        return ApiException.notFound("ASSIGNMENT_NOT_FOUND");
    }

    private boolean allowed(Actor actor, String action, ResourceRef resource) {
        AuthorizationDecision decision;
        try {
            decision = authorization.authorize(actor, action, resource);
        } catch (ApiException e) {
            throw e;
        } catch (RuntimeException e) {
            throw unavailable();
        }
        if (decision == null) {
            throw unavailable();
        }
        return decision.allowed();
    }

    private static void requireSignedIn(Actor actor) {
        if (actor == null || actor.accountId() == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "Cần đăng nhập");
        }
    }

    private static ApiException unavailable() {
        return ApiException.unavailable("ASSIGNMENT_DEPENDENCY_UNAVAILABLE", "Chưa kiểm tra được quyền, vui lòng thử lại");
    }
}
