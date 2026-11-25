package edu.aiplatform.groups.service;

import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.academics.contract.IClassAccess;
import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.AuthorizationDecision;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.ResourceRef;
import edu.aiplatform.shared.error.ApiException;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Kiểm quyền chung của U12 qua U01 và U05: giảng viên chính của lớp (R3/R4, BR-U12-01) hoặc sinh viên đang học
 * (R5). Ngoài phạm vi trả {@code 404} (không ghi audit từ 2026-11-25); U01, U05 lỗi thì từ chối (BR-U12-62).
 */
@Component
public class GroupAccess {

    public static final String NOT_FOUND = "GROUP_NOT_FOUND";

    private final IAuthorization authorization;
    private final IClassAccess classes;

    public GroupAccess(IAuthorization authorization, IClassAccess classes) {
        this.authorization = authorization;
        this.classes = classes;
    }

    /** Giảng viên chính của lớp; trả thông tin lớp. */
    public ClassRef requireTeacher(Actor actor, UUID classId) {
        require(actor, classId, Actions.GROUP_MANAGE);
        return loadClassRef(classId);
    }

    /** Sinh viên đang học của lớp; trả thông tin lớp. */
    public ClassRef requireStudent(Actor actor, UUID classId) {
        require(actor, classId, Actions.GROUP_WORK);
        boolean active;
        try {
            active = classes.isActiveStudent(actor.accountId(), classId);
        } catch (RuntimeException e) {
            throw createUnavailableError();
        }
        if (!active) {
            throw createNotFoundError();
        }
        return loadClassRef(classId);
    }

    /** Có được làm thao tác không (không audit), dùng khi chỉ cần biết để hiện nút hay kiểm vai trò phụ. */
    public boolean isAllowed(Actor actor, UUID classId, String action) {
        try {
            AuthorizationDecision decision = authorization.authorize(actor, action, ResourceRef.courseClass(classId));
            return decision != null && decision.allowed();
        } catch (RuntimeException e) {
            return false;
        }
    }

    public ClassRef loadClassRef(UUID classId) {
        try {
            return classes.getClassRef(classId).orElseThrow(GroupAccess::createNotFoundError);
        } catch (ApiException e) {
            throw e;
        } catch (RuntimeException e) {
            throw createUnavailableError();
        }
    }

    /** Sửa nhóm và xử lý yêu cầu chỉ khi lớp {@code DRAFT} hoặc {@code OPEN} (BR-U12-06). */
    public static void requireEditable(ClassRef ref) {
        if (ref.status() == ClassStatus.ARCHIVED) {
            throw ApiException.createConflict("GROUP_CLASS_ARCHIVED", "Lớp đã lưu trữ, chỉ xem được nhóm");
        }
    }

    /** Sinh viên chỉ thao tác ở lớp {@code OPEN} (BR-U12-06). */
    public static void requireOpen(ClassRef ref) {
        if (ref.status() != ClassStatus.OPEN) {
            throw ApiException.createConflict("GROUP_CLASS_NOT_OPEN", "Lớp không mở, chưa thao tác được");
        }
    }

    public static ApiException createNotFoundError() {
        return ApiException.createNotFound(NOT_FOUND);
    }

    private void require(Actor actor, UUID classId, String action) {
        AuthorizationDecision decision;
        try {
            decision = authorization.authorize(actor, action, ResourceRef.courseClass(classId));
        } catch (RuntimeException e) {
            throw createUnavailableError();
        }
        if (decision == null || "DEPENDENCY_UNAVAILABLE".equals(decision.reason())) {
            throw createUnavailableError();
        }
        if (!decision.allowed()) {
            throw createNotFoundError();
        }
    }

    /** Đề xuất nhóm không thuộc phạm vi của người gọi: trả {@code 404} (BR-U12-91). */
    public ApiException createProposalNotFoundError() {
        return ApiException.createNotFound("GROUP_PROPOSAL_NOT_FOUND");
    }

    private static ApiException createUnavailableError() {
        return ApiException.createUnavailable("GROUP_DEPENDENCY_UNAVAILABLE", "Hệ thống tạm bận, vui lòng thử lại sau");
    }
}
