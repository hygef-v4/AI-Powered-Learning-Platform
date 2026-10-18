package edu.aiplatform.identity.application;

import java.util.List;
import java.util.UUID;

/**
 * Role mới không giữ được phân công hiện có (BR-U01-64): trả 409 {@code ROLE_CHANGE_BLOCKED} kèm môn đang làm Chủ nhiệm
 * và lớp đang dạy để Admin xử lý phân công trước.
 */
public class RoleChangeBlockedException extends RuntimeException {

    private final List<UUID> managedSubjects;
    private final List<UUID> taughtClasses;

    public RoleChangeBlockedException(List<UUID> managedSubjects, List<UUID> taughtClasses) {
        super("Không đổi được role vì tài khoản còn phụ trách môn hoặc lớp");
        this.managedSubjects = List.copyOf(managedSubjects);
        this.taughtClasses = List.copyOf(taughtClasses);
    }

    public List<UUID> managedSubjects() {
        return managedSubjects;
    }

    public List<UUID> taughtClasses() {
        return taughtClasses;
    }
}
