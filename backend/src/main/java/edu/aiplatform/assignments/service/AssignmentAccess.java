package edu.aiplatform.assignments.service;

import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.academics.contract.IClassAccess;
import edu.aiplatform.assignments.contract.AssignmentScope;
import edu.aiplatform.assignments.model.Assignment;
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
 * đã phát hành. Ngoài phạm vi trả 404, không ghi audit (BR-U02-10); U01, U04, U05 lỗi thì từ chối (fail closed).
 */
@Component
class AssignmentAccess {

    private final IAuthorization authorization;
    private final IClassScope classScope;
    private final IClassAccess classes;
    private final ISubjectQuery subjects;

    AssignmentAccess(IAuthorization authorization, IClassScope classScope, IClassAccess classes,
                     ISubjectQuery subjects) {
        this.authorization = authorization;
        this.classScope = classScope;
        this.classes = classes;
        this.subjects = subjects;
    }

    /** Giảng viên chính của lớp; trả lớp để kiểm trạng thái. */
    ClassRef requireClassManager(Actor actor, UUID classId, String action) {
        requireSignedIn(actor);
        if (!isAllowed(actor, Actions.ASSIGNMENT_CLASS_MANAGE, ResourceRef.courseClass(classId))) {
            throw notFound();
        }
        return loadClassRef(classId);
    }

    void requireSubjectManager(Actor actor, UUID subjectId, String action) {
        requireSignedIn(actor);
        if (!isAllowed(actor, Actions.ASSIGNMENT_SUBJECT_MANAGE, ResourceRef.subject(subjectId))) {
            throw notFound();
        }
    }

    boolean canManage(Actor actor, Assignment assignment) {
        if (actor == null || actor.accountId() == null) {
            return false;
        }
        return assignment.scope() == AssignmentScope.CLASS
                ? isAllowed(actor, Actions.ASSIGNMENT_CLASS_MANAGE, ResourceRef.courseClass(assignment.classId()))
                : isAllowed(actor, Actions.ASSIGNMENT_SUBJECT_MANAGE, ResourceRef.subject(assignment.subjectId()));
    }

    void requireManage(Actor actor, Assignment assignment, String action) {
        requireSignedIn(actor);
        if (!canManage(actor, assignment)) {
            throw notFound();
        }
    }

    /** Người soạn, hoặc giảng viên một lớp của môn với bài của môn đã phát hành (chỉ xem). */
    boolean canRead(Actor actor, Assignment assignment) {
        if (canManage(actor, assignment)) {
            return true;
        }
        if (assignment.scope() != AssignmentScope.SUBJECT || !assignment.hasPublished()
                || actor.role() == Role.STUDENT || actor.role() == Role.ADMIN) {
            return false;
        }
        for (UUID classId : classScope.listTaughtClasses(actor.accountId())) {
            if (classScope.subjectOfClass(classId).map(assignment.subjectId()::equals).orElse(false)
                    && isAllowed(actor, Actions.ASSIGNMENT_CLASS_MANAGE, ResourceRef.courseClass(classId))) {
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
            throw createUnavailableError();
        }
        if (!readable) {
            throw notFound();
        }
    }

    /** Lớp hoặc môn đã lưu trữ thì bài chỉ đọc (BR-U07-04). */
    boolean isOwnerArchived(Assignment assignment) {
        if (assignment.scope() == AssignmentScope.CLASS) {
            return loadClassRef(assignment.classId()).status() == ClassStatus.ARCHIVED;
        }
        return loadSubjectStatus(assignment.subjectId()) == SubjectStatus.ARCHIVED;
    }

    void requireOwnerWritable(Assignment assignment) {
        if (isOwnerArchived(assignment)) {
            throw ApiException.createConflict("ASSIGNMENT_READ_ONLY", assignment.scope() == AssignmentScope.CLASS
                    ? "Lớp đã lưu trữ, bài chỉ xem được" : "Môn đã lưu trữ, bài chỉ xem được");
        }
    }

    ClassRef loadClassRef(UUID classId) {
        try {
            return classes.getClassRef(classId).orElseThrow(() -> ApiException.createNotFound("CLASS_NOT_FOUND"));
        } catch (ApiException e) {
            throw e;
        } catch (RuntimeException e) {
            throw createUnavailableError();
        }
    }

    edu.aiplatform.subjects.contract.SubjectRef loadSubjectRef(UUID subjectId) {
        try {
            return subjects.getSubject(subjectId).orElseThrow(() -> ApiException.createNotFound("SUBJECT_NOT_FOUND"));
        } catch (ApiException e) {
            throw e;
        } catch (RuntimeException e) {
            throw createUnavailableError();
        }
    }

    SubjectStatus loadSubjectStatus(UUID subjectId) {
        try {
            return subjects.getSubject(subjectId).orElseThrow(() -> ApiException.createNotFound("SUBJECT_NOT_FOUND"))
                    .status();
        } catch (ApiException e) {
            throw e;
        } catch (RuntimeException e) {
            throw createUnavailableError();
        }
    }

    ApiException notFound() {
        return ApiException.createNotFound("ASSIGNMENT_NOT_FOUND");
    }

    private boolean isAllowed(Actor actor, String action, ResourceRef resource) {
        AuthorizationDecision decision;
        try {
            decision = authorization.authorize(actor, action, resource);
        } catch (ApiException e) {
            throw e;
        } catch (RuntimeException e) {
            throw createUnavailableError();
        }
        // U01 không kiểm được phạm vi (database, U04, U05 lỗi): báo bận, không trả 404 và ghi từ chối oan.
        if (decision == null || (!decision.allowed() && "DEPENDENCY_UNAVAILABLE".equals(decision.reason()))) {
            throw createUnavailableError();
        }
        return decision.allowed();
    }

    private static void requireSignedIn(Actor actor) {
        if (actor == null || actor.accountId() == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "Cần đăng nhập");
        }
    }

    private static ApiException createUnavailableError() {
        return ApiException.createUnavailable("ASSIGNMENT_DEPENDENCY_UNAVAILABLE", "Chưa kiểm tra được quyền, vui lòng thử lại");
    }
}
