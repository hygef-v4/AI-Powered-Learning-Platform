package edu.aiplatform.academics.contract;

import java.util.UUID;

/**
 * {@code subjectCode}: mã môn của lớp, để giao diện ghi mã môn trên mã lớp (yêu cầu 2026-11-02 bỏ tên lớp); có thể null ở
 * chỗ tạo tay (stub, test). {@code term}: học kỳ suy ra từ ngày bắt đầu (D-U04-05), ví dụ {@code SP27-BL5}; có thể null ở
 * chỗ tạo tay.
 */
public record ClassRef(UUID classId, UUID subjectId, String code, String name, ClassStatus status,
        UUID teacherId, boolean showGradeDistribution, String subjectCode, String term) {

    /** Creates a class reference without a term (stubs and hand-built fixtures). */
    public ClassRef(UUID classId, UUID subjectId, String code, String name, ClassStatus status,
            UUID teacherId, boolean showGradeDistribution, String subjectCode) {
        this(classId, subjectId, code, name, status, teacherId, showGradeDistribution, subjectCode,
                null);
    }

    /** Validates the class identity and normalizes its display fields. */
    public ClassRef(UUID classId, UUID subjectId, String code, String name, ClassStatus status,
            UUID teacherId, boolean showGradeDistribution) {
        this(classId, subjectId, code, name, status, teacherId, showGradeDistribution, null, null);
    }
}
