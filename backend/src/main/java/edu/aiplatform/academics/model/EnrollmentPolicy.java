package edu.aiplatform.academics.model;

import edu.aiplatform.academics.contract.ClassStatus;
import java.util.Objects;

/** Chỉ đánh giá dữ kiện U05; caller chịu trách nhiệm khóa, kiểm quyền và điều kiện tài khoản. */
public final class EnrollmentPolicy {
    private EnrollmentPolicy() { }

    public static EnrollmentResult resultFor(ClassStatus status, Enrollment current, boolean activeInOtherClass) {
        Objects.requireNonNull(status);
        if (status == ClassStatus.ARCHIVED) throw new AcademicRuleException("CLASS_ARCHIVED", "Không ghi danh vào lớp đã lưu trữ");
        if (current != null && current.status() == EnrollmentStatus.ACTIVE) return EnrollmentResult.ALREADY_ENROLLED;
        if (activeInOtherClass) return EnrollmentResult.IN_OTHER_CLASS;
        return current == null ? EnrollmentResult.ENROLLED : EnrollmentResult.RESTORED;
    }
}
