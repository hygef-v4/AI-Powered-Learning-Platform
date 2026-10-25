package edu.aiplatform.identity.application;

import java.util.List;
import java.util.UUID;

/**
 * Role mới không giữ được phân công hiện có (BR-U01-64): trả 409 {@code ROLE_CHANGE_BLOCKED} kèm môn đang làm Chủ nhiệm
 * và lớp đang dạy (mã, tên) để Admin biết phải chuyển phân công nào trước.
 */
public class RoleChangeBlockedException extends RuntimeException {

    /** Môn hoặc lớp còn giữ; {@code code}, {@code name} là {@code null} khi unit sở hữu chưa trả được. */
    public record Held(UUID id, String code, String name) {
    }

    private final List<Held> subjects;
    private final List<Held> classes;

    public RoleChangeBlockedException(List<Held> subjects, List<Held> classes) {
        super("Không đổi được role vì tài khoản còn phụ trách môn hoặc lớp");
        this.subjects = List.copyOf(subjects);
        this.classes = List.copyOf(classes);
    }

    public List<Held> subjects() {
        return subjects;
    }

    public List<Held> classes() {
        return classes;
    }

    public List<UUID> managedSubjects() {
        return subjects.stream().map(Held::id).toList();
    }

    public List<UUID> taughtClasses() {
        return classes.stream().map(Held::id).toList();
    }
}
