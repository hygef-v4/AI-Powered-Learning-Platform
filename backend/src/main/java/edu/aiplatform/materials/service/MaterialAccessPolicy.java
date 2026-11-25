package edu.aiplatform.materials.service;

import edu.aiplatform.academics.contract.*;
import edu.aiplatform.audit.contract.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.materials.model.Material;
import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.subjects.contract.*;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
public class MaterialAccessPolicy {
    private final IAuthorization authorization;
    private final ISubjectQuery subjectQuery;
    private final IClassAccess classAccess;
    private final IClassScope classScope;

    public MaterialAccessPolicy(
            IAuthorization authorization,
            ISubjectQuery subjectQuery,
            IClassAccess classAccess,
            IClassScope classScope) {
        this.authorization = authorization;
        this.subjectQuery = subjectQuery;
        this.classAccess = classAccess;
        this.classScope = classScope;
    }

    private <T> T callDependency(Supplier<T> operation) {
        try {
            return operation.get();
        } catch (ApiException e) {
            throw e;
        } catch (RuntimeException e) {
            throw ApiException.createUnavailable(
                    "MATERIAL_DEPENDENCY_UNAVAILABLE",
                    "Không kiểm được quyền học liệu, vui lòng thử lại");
        }
    }

    private boolean isAllowed(Actor actor, String action, ResourceRef ref) {
        if (actor == null || actor.accountId() == null || actor.role() == null) return false;
        var decision = callDependency(() -> authorization.authorize(actor, action, ref));
        if (decision == null || "DEPENDENCY_UNAVAILABLE".equals(decision.reason()))
            throw ApiException.createUnavailable(
                    "MATERIAL_AUTH_UNAVAILABLE",
                    "Không kiểm được quyền học liệu, vui lòng thử lại");
        return decision.allowed();
    }

    public SubjectRef requireSubject(UUID id) {
        return callDependency(() -> subjectQuery.getSubject(id))
                .orElseThrow(() -> ApiException.createNotFound("MATERIAL_NOT_FOUND"));
    }

    public ClassRef requireClass(UUID id) {
        return callDependency(() -> classAccess.getClassRef(id))
                .orElseThrow(() -> ApiException.createNotFound("MATERIAL_NOT_FOUND"));
    }

    private boolean isSubjectManager(Actor actor, UUID id) {
        return actor != null
                && actor.role() == Role.SUBJECT_MANAGER
                && isAllowed(actor, Actions.MATERIAL_SUBJECT_MANAGE, ResourceRef.subject(id));
    }

    /** Chủ nhiệm môn sửa được module của môn đang hoạt động. */
    public void requireModuleManagement(Actor actor, UUID subjectId) {
        if (!isSubjectManager(actor, subjectId)) denyAccess(actor, subjectId);
        if (requireSubject(subjectId).status() != SubjectStatus.ACTIVE)
            throw ApiException.createConflict(
                    "MATERIAL_SCOPE_READ_ONLY", "Môn đã lưu trữ, học liệu chỉ đọc");
    }

    public boolean canManageModules(Actor actor, UUID subjectId) {
        return isSubjectManager(actor, subjectId)
                && requireSubject(subjectId).status() == SubjectStatus.ACTIVE;
    }

    /**
     * Gắn, gỡ Quiz khỏi module: Quiz của môn do Chủ nhiệm môn, Quiz của lớp do giảng viên của lớp
     * (lớp chưa lưu trữ).
     */
    public boolean canManageQuiz(Actor actor, UUID subjectId, UUID classId) {
        if (classId == null) return canManageModules(actor, subjectId);
        ClassRef c = requireClass(classId);
        return subjectId.equals(c.subjectId())
                && c.status() != ClassStatus.ARCHIVED
                && canStaffRead(actor, c);
    }

    public void requireSubjectListAccess(Actor actor, UUID subjectId) {
        if (!isSubjectManager(actor, subjectId)) denyAccess(actor, subjectId);
        requireSubject(subjectId);
    }

    public ClassRef requireClassListAccess(Actor actor, UUID classId, boolean student) {
        ClassRef c = requireClass(classId);
        boolean ok = student ? canStudentRead(actor, c) : canStaffRead(actor, c);
        if (!ok) denyAccess(actor, classId);
        return c;
    }

    private boolean canStaffRead(Actor actor, ClassRef courseClass) {
        return actor != null
                && (actor.role() == Role.TEACHER || actor.role() == Role.SUBJECT_MANAGER)
                && actor.accountId().equals(courseClass.teacherId())
                && isAllowed(
                        actor,
                        Actions.MATERIAL_STAFF_READ,
                        ResourceRef.courseClass(courseClass.classId()));
    }

    private boolean canStudentRead(Actor actor, ClassRef courseClass) {
        return actor != null
                && actor.role() == Role.STUDENT
                && courseClass.status() == ClassStatus.OPEN
                && isAllowed(
                        actor,
                        Actions.MATERIAL_STUDENT_READ,
                        ResourceRef.courseClass(courseClass.classId()))
                && callDependency(
                        () -> classAccess.isActiveStudent(
                                actor.accountId(), courseClass.classId()));
    }

    public boolean canManageMaterial(Actor actor, Material material) {
        if (material.scope() == Material.Scope.SUBJECT)
            return isSubjectManager(actor, material.subjectId());
        ClassRef c = requireClass(material.classId());
        return Objects.equals(c.subjectId(), material.subjectId()) && canStaffRead(actor, c);
    }

    public void requireWriteAccess(Actor actor, Material material) {
        if (!canManageMaterial(actor, material)) denyAccess(actor, material.id());
        if (requireSubject(material.subjectId()).status() != SubjectStatus.ACTIVE
                || (material.classId() != null
                        && requireClass(material.classId()).status() == ClassStatus.ARCHIVED))
            throw ApiException.createConflict(
                    "MATERIAL_SCOPE_READ_ONLY", "Môn hoặc lớp đã lưu trữ, học liệu chỉ đọc");
        if (material.status() != Material.Status.ACTIVE)
            throw ApiException.createConflict("MATERIAL_ARCHIVED", "Học liệu đã được lưu trữ");
    }

    public void requireReadAccess(Actor actor, Material material, UUID classId) {
        if (actor == null || actor.role() == null || actor.role() == Role.ADMIN) {
            denyAccess(actor, material.id());
            return;
        }
        if (actor.role() == Role.STUDENT) {
            if (material.status() != Material.Status.ACTIVE || !material.enabled()) {
                denyAccess(actor, material.id());
                return;
            }
            List<ClassRef> candidates =
                    classId == null
                            ? callDependency(() -> classAccess.listOpenClassesOf(actor.accountId()))
                            : List.of(requireClass(classId));
            for (ClassRef c : candidates)
                if (isMaterialOfClass(material, c) && canStudentRead(actor, c)) return;
        } else {
            if (material.scope() == Material.Scope.SUBJECT
                    && isSubjectManager(actor, material.subjectId())) return;
            if (material.scope() == Material.Scope.CLASS) {
                ClassRef c = requireClass(material.classId());
                if ((classId == null || classId.equals(c.classId()))
                        && isMaterialOfClass(material, c)
                        && canStaffRead(actor, c)) return;
            } else if (classId != null) {
                ClassRef c = requireClass(classId);
                if (isMaterialOfClass(material, c) && canStaffRead(actor, c)) return;
            } else {
                List<UUID> taughtClassIds =
                        callDependency(() -> classScope.listTaughtClasses(actor.accountId()));
                for (UUID id : taughtClassIds) {
                    ClassRef c = requireClass(id);
                    if (isMaterialOfClass(material, c) && canStaffRead(actor, c)) return;
                }
            }
        }
        denyAccess(actor, material.id());
    }

    private boolean isMaterialOfClass(Material material, ClassRef courseClass) {
        return material.subjectId().equals(courseClass.subjectId())
                && (material.scope() == Material.Scope.SUBJECT
                        || material.classId().equals(courseClass.classId()));
    }

    private void denyAccess(Actor actor, UUID id) {
        throw ApiException.createNotFound("MATERIAL_NOT_FOUND");
    }
}
