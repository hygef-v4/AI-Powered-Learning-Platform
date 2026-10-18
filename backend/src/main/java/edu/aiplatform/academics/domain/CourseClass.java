package edu.aiplatform.academics.domain;

import edu.aiplatform.academics.contract.ClassStatus;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Snapshot bất biến. Quyền, role tài khoản và trạng thái môn phải được application xác minh trước. */
public record CourseClass(UUID id, UUID subjectId, String code, String name, String description, String term,
                          ClassStatus status, UUID teacherId, boolean showGradeDistribution,
                          Instant createdAt, Instant updatedAt, long version) {
    public CourseClass {
        Objects.requireNonNull(id);
        Objects.requireNonNull(subjectId);
        Objects.requireNonNull(status);
        Objects.requireNonNull(createdAt);
        Objects.requireNonNull(updatedAt);
        code = text(code, "Mã lớp", 30, true);
        name = text(name, "Tên lớp", 200, true);
        description = text(description, "Mô tả", 2000, false);
        term = text(term, "Học kỳ", 20, false);
        if (version < 0 || updatedAt.isBefore(createdAt)) {
            throw new AcademicRuleException("INVALID_CLASS_SNAPSHOT", "Version hoặc thời gian lớp không hợp lệ");
        }
        if (status == ClassStatus.OPEN && teacherId == null) {
            throw new AcademicRuleException("TEACHER_REQUIRED", "Lớp mở cần giảng viên chính");
        }
    }

    public static CourseClass create(UUID id, UUID subjectId, String code, String name, String description,
                                     String term, UUID teacherId, Instant now) {
        return new CourseClass(id, subjectId, code, name, description, term, ClassStatus.DRAFT, teacherId,
                false, now, now, 0);
    }

    /** Hai dữ kiện cross-unit phải được lấy trong transaction có khóa; không phải bằng chứng authorization. */
    public CourseClass changeStatus(ClassStatus next, boolean subjectActive, boolean conflictingEnrollments, Instant now) {
        Objects.requireNonNull(next);
        boolean valid = switch (status) {
            case DRAFT -> next == ClassStatus.OPEN || next == ClassStatus.ARCHIVED;
            case OPEN -> next == ClassStatus.ARCHIVED;
            case ARCHIVED -> next == ClassStatus.OPEN;
        };
        if (!valid) throw new AcademicRuleException("INVALID_CLASS_TRANSITION", "Không thể chuyển trạng thái lớp này");
        if (next == ClassStatus.OPEN) {
            if (teacherId == null) throw new AcademicRuleException("TEACHER_REQUIRED", "Cần phân công giảng viên trước khi mở lớp");
            if (!subjectActive) throw new AcademicRuleException("SUBJECT_INACTIVE", "Chỉ mở lớp khi môn đang hoạt động");
            if (conflictingEnrollments) {
                throw new AcademicRuleException("ENROLLMENT_CONFLICT", "Có sinh viên đang ở lớp khác cùng môn");
            }
        }
        return changed(name, description, term, next, teacherId, showGradeDistribution, now);
    }

    public CourseClass updateInfo(String name, String description, String term, Instant now) {
        requireEditable();
        return changed(name, description, term, status, teacherId, showGradeDistribution, now);
    }

    public CourseClass assignTeacher(UUID teacherId, Instant now) {
        requireEditable();
        return changed(name, description, term, status, teacherId, showGradeDistribution, now);
    }

    public CourseClass setGradeDistribution(boolean enabled, Instant now) {
        requireEditable();
        return changed(name, description, term, status, teacherId, enabled, now);
    }

    private void requireEditable() {
        if (status == ClassStatus.ARCHIVED) throw new AcademicRuleException("CLASS_ARCHIVED", "Lớp đã lưu trữ chỉ được xem");
    }

    private CourseClass changed(String name, String description, String term, ClassStatus status, UUID teacher,
                                boolean distribution, Instant now) {
        if (Objects.requireNonNull(now).isBefore(updatedAt)) {
            throw new AcademicRuleException("INVALID_CLASS_TIME", "Thời gian cập nhật không thể trước lần cập nhật trước");
        }
        return new CourseClass(id, subjectId, code, name, description, term, status, teacher, distribution,
                createdAt, now, Math.addExact(version, 1));
    }

    private static String text(String value, String label, int max, boolean required) {
        String normalized = value == null ? null : value.strip();
        if ((required && (normalized == null || normalized.isEmpty())) ||
                (normalized != null && normalized.length() > max) ||
                (normalized != null && normalized.codePoints().anyMatch(c -> Character.isISOControl(c) && c != '\n' && c != '\t'))) {
            throw new AcademicRuleException("INVALID_CLASS_FIELD", label + " không hợp lệ (tối đa " + max + " ký tự)");
        }
        return normalized;
    }
}
