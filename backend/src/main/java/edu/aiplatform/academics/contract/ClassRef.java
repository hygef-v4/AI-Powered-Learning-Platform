package edu.aiplatform.academics.contract;

import java.util.UUID;

/**
 * {@code subjectCode}: mã môn của lớp, để giao diện ghi mã môn trên mã lớp (yêu cầu 2026-11-02 bỏ tên lớp); có thể null ở
 * chỗ tạo tay (stub, test).
 */
public record ClassRef(UUID classId, UUID subjectId, String code, String name, ClassStatus status,
        UUID teacherId, boolean showGradeDistribution, String subjectCode) {

    /** Validates the class identity and normalizes its display fields. */
    public ClassRef(UUID classId, UUID subjectId, String code, String name, ClassStatus status,
            UUID teacherId, boolean showGradeDistribution) {
        this(classId, subjectId, code, name, status, teacherId, showGradeDistribution, null);
    }
}
