package edu.aiplatform.groups.service;

import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.academics.contract.ClassStatus;
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

/**
 * Kiểm quyền chung của U12 qua U01 và U05: giảng viên chính của lớp (R3/R4, BR-U12-01) hoặc sinh viên đang học
 * (R5). Ngoài phạm vi trả {@code 404} và ghi audit {@code ACCESS_DENIED}; U01, U05 lỗi thì từ chối (BR-U12-62).
 */
@Component
public class GroupAccess {

    public static final String NOT_FOUND = "GROUP_NOT_FOUND";

    private final IAuthorization authorization;
    private final IClassAccess classes;
    private final IAudit audit;
    private final Clock clock;

    public GroupAccess(IAuthorization authorization, IClassAccess classes, IAudit audit, Clock clock) {
        this.authorization = authorization;
        this.classes = classes;
        this.audit = audit;
        this.clock = clock;
    }

    /** Giảng viên chính của lớp; trả thông tin lớp. */
    public ClassRef requireTeacher(Actor actor, UUID classId) {
        require(actor, classId, Actions.GROUP_MANAGE);
        return classRef(classId);
    }

    /** Sinh viên đang học của lớp; trả thông tin lớp. */
    public ClassRef requireStudent(Actor actor, UUID classId) {
        require(actor, classId, Actions.GROUP_WORK);
        boolean active;
        try {
            active = classes.isActiveStudent(actor.accountId(), classId);
        } catch (RuntimeException e) {
            throw unavailable();
        }
        if (!active) {
            deny(actor, classId, Actions.GROUP_WORK, "ENROLLMENT_REQUIRED");
        }
        return classRef(classId);
    }

    /** Có được làm thao tác không (không audit), dùng khi chỉ cần biết để hiện nút hay kiểm vai trò phụ. */
    public boolean allowed(Actor actor, UUID classId, String action) {
        try {
            AuthorizationDecision decision = authorization.authorize(actor, action, ResourceRef.courseClass(classId));
            return decision != null && decision.allowed();
        } catch (RuntimeException e) {
            return false;
        }
    }

    public ClassRef classRef(UUID classId) {
        try {
            return classes.getClassRef(classId).orElseThrow(GroupAccess::notFound);
        } catch (ApiException e) {
            throw e;
        } catch (RuntimeException e) {
            throw unavailable();
        }
    }

    /** Sửa nhóm và xử lý yêu cầu chỉ khi lớp {@code DRAFT} hoặc {@code OPEN} (BR-U12-06). */
    public static void requireEditable(ClassRef ref) {
        if (ref.status() == ClassStatus.ARCHIVED) {
            throw ApiException.conflict("GROUP_CLASS_ARCHIVED", "Lớp đã lưu trữ, chỉ xem được nhóm");
        }
    }

    /** Sinh viên chỉ thao tác ở lớp {@code OPEN} (BR-U12-06). */
    public static void requireOpen(ClassRef ref) {
        if (ref.status() != ClassStatus.OPEN) {
            throw ApiException.conflict("GROUP_CLASS_NOT_OPEN", "Lớp không mở, chưa thao tác được");
        }
    }

    public static ApiException notFound() {
        return ApiException.notFound(NOT_FOUND);
    }

    private void require(Actor actor, UUID classId, String action) {
        AuthorizationDecision decision;
        try {
            decision = authorization.authorize(actor, action, ResourceRef.courseClass(classId));
        } catch (RuntimeException e) {
            throw unavailable();
        }
        if (decision == null || "DEPENDENCY_UNAVAILABLE".equals(decision.reason())) {
            throw unavailable();
        }
        if (!decision.allowed()) {
            deny(actor, classId, action, decision.reason());
        }
    }

    private void deny(Actor actor, UUID classId, String action, String reason) {
        audit.record(new AuditEvent(UUID.randomUUID(), actor == null ? null : actor.accountId(), "ACCESS_DENIED", "CLASS",
                String.valueOf(classId), AuditResult.DENIED, reason, Map.of("action", action), clock.instant()));
        throw notFound();
    }

    private static ApiException unavailable() {
        return ApiException.unavailable("GROUP_DEPENDENCY_UNAVAILABLE", "Hệ thống tạm bận, vui lòng thử lại sau");
    }
}
