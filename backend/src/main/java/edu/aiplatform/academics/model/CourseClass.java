package edu.aiplatform.academics.model;

import edu.aiplatform.academics.contract.ClassStatus;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Snapshot bất biến. Quyền, role tài khoản và trạng thái môn phải được application xác minh trước. */
public record CourseClass(UUID id, UUID subjectId, String code, String name, String description,
        String term, ClassStatus status, UUID teacherId, boolean showGradeDistribution,
        Instant createdAt, Instant updatedAt, long version) {
    /** Validates and normalizes the immutable class snapshot. */
    public CourseClass {
        Objects.requireNonNull(id);
        Objects.requireNonNull(subjectId);
        Objects.requireNonNull(status);
        Objects.requireNonNull(createdAt);
        Objects.requireNonNull(updatedAt);
        code = requireText(code, "Mã lớp", 30, true);
        name = requireText(name, "Tên lớp", 200, true);
        description = requireText(description, "Mô tả", 2000, false);
        term = requireText(term, "Học kỳ", 20, false);
        if (version < 0 || updatedAt.isBefore(createdAt)) {
            throw new AcademicRuleException("INVALID_CLASS_SNAPSHOT",
                    "Version hoặc thời gian lớp không hợp lệ");
        }
    }

    /** Creates a draft class with version zero and grade distribution hidden. */
    public static CourseClass createClass(UUID id, UUID subjectId, String code, String name,
            String description, String term, UUID teacherId, Instant now) {
        return new CourseClass(id, subjectId, code, name, description, term, ClassStatus.DRAFT,
                teacherId, false, now, now, 0);
    }

    /** Changes class status using locked subject and enrollment facts after authorization. */
    public CourseClass changeStatus(ClassStatus next, boolean subjectActive,
            boolean conflictingEnrollments, Instant now) {
        Objects.requireNonNull(next);
        boolean valid = switch (status) {
        case DRAFT -> next == ClassStatus.OPEN || next == ClassStatus.ARCHIVED;
        case OPEN -> next == ClassStatus.ARCHIVED;
        case ARCHIVED -> next == ClassStatus.OPEN;
        };
        if (!valid)
            throw new AcademicRuleException("INVALID_CLASS_TRANSITION",
                    "Không thể chuyển trạng thái lớp này");
        if (next == ClassStatus.OPEN) {
            if (!subjectActive)
                throw new AcademicRuleException("SUBJECT_INACTIVE",
                        "Chỉ mở lớp khi môn đang hoạt động");
            if (conflictingEnrollments) {
                throw new AcademicRuleException("ENROLLMENT_CONFLICT",
                        "Có sinh viên đang ở lớp khác cùng môn");
            }
        }
        return copyWithChanges(name, description, term, next, teacherId, showGradeDistribution,
                now);
    }

    /** Updates basic class metadata (name, description, term) while incrementing version. */
    public CourseClass updateInfo(String name, String description, String term, Instant now) {
        requireEditable();
        return copyWithChanges(name, description, term, status, teacherId, showGradeDistribution,
                now);
    }

    /** Reassigns the responsible teacher to this class while incrementing version. */
    public CourseClass assignTeacher(UUID teacherId, Instant now) {
        requireEditable();
        return copyWithChanges(name, description, term, status, teacherId, showGradeDistribution,
                now);
    }

    /** Sets whether students can see the grade distribution. */
    public CourseClass setGradeDistribution(boolean enabled, Instant now) {
        requireEditable();
        return copyWithChanges(name, description, term, status, teacherId, enabled, now);
    }

    /** Enforces that the class is in a mutable status (not ARCHIVED). */
    private void requireEditable() {
        if (status == ClassStatus.ARCHIVED)
            throw new AcademicRuleException("CLASS_ARCHIVED", "Lớp đã lưu trữ chỉ được xem");
    }

    /** Copies the class with updated fields and refreshed modification metadata. */
    private CourseClass copyWithChanges(String name, String description, String term,
            ClassStatus status, UUID teacher, boolean distribution, Instant now) {
        if (Objects.requireNonNull(now).isBefore(updatedAt)) {
            throw new AcademicRuleException("INVALID_CLASS_TIME",
                    "Thời gian cập nhật không thể trước lần cập nhật trước");
        }
        return new CourseClass(id, subjectId, code, name, description, term, status, teacher,
                distribution, createdAt, now, Math.addExact(version, 1));
    }

    /** Validates required and maximum-length text fields. */
    private static String requireText(String value, String label, int max, boolean required) {
        String normalized = value == null ? null : value.strip();
        if ((required && (normalized == null || normalized.isEmpty()))
                || (normalized != null && normalized.length() > max)
                || (normalized != null && normalized.codePoints()
                        .anyMatch(c -> Character.isISOControl(c) && c != '\n' && c != '\t'))) {
            throw new AcademicRuleException("INVALID_CLASS_FIELD",
                    label + " không hợp lệ (tối đa " + max + " ký tự)");
        }
        return normalized;
    }
}
