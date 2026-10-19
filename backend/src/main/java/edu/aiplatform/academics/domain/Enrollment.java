package edu.aiplatform.academics.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Enrollment(UUID classId, UUID accountId, EnrollmentStatus status, EnrollmentSource source,
                         Instant enrolledAt, Instant removedAt) {
    public Enrollment {
        Objects.requireNonNull(classId);
        Objects.requireNonNull(accountId);
        Objects.requireNonNull(status);
        Objects.requireNonNull(source);
        Objects.requireNonNull(enrolledAt);
        if ((status == EnrollmentStatus.ACTIVE && removedAt != null) ||
                (status == EnrollmentStatus.REMOVED && (removedAt == null || removedAt.isBefore(enrolledAt)))) {
            throw new AcademicRuleException("INVALID_ENROLLMENT", "Thời gian gỡ ghi danh không hợp lệ");
        }
    }

    public static Enrollment create(UUID classId, UUID accountId, EnrollmentSource source, Instant now) {
        return new Enrollment(classId, accountId, EnrollmentStatus.ACTIVE, source, now, null);
    }

    public Enrollment remove(Instant now) {
        return status == EnrollmentStatus.REMOVED ? this :
                new Enrollment(classId, accountId, EnrollmentStatus.REMOVED, source, enrolledAt, Objects.requireNonNull(now));
    }

    public Enrollment restore(EnrollmentSource source, Instant now) {
        if (status == EnrollmentStatus.ACTIVE) throw new AcademicRuleException("ALREADY_ENROLLED", "Sinh viên đã được ghi danh");
        if (Objects.requireNonNull(now).isBefore(removedAt)) {
            throw new AcademicRuleException("INVALID_ENROLLMENT_TIME", "Thời gian khôi phục không hợp lệ");
        }
        return create(classId, accountId, source, now);
    }
}
