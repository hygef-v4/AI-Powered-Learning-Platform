package edu.aiplatform.academics.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Enrollment(UUID classId, UUID accountId, EnrollmentStatus status,
        EnrollmentSource source, Instant enrolledAt, Instant removedAt) {
    /** Validates enrollment state and lifecycle timestamps. */
    public Enrollment {
        Objects.requireNonNull(classId);
        Objects.requireNonNull(accountId);
        Objects.requireNonNull(status);
        Objects.requireNonNull(source);
        Objects.requireNonNull(enrolledAt);
        if ((status == EnrollmentStatus.ACTIVE && removedAt != null)
                || (status == EnrollmentStatus.REMOVED
                        && (removedAt == null || removedAt.isBefore(enrolledAt)))) {
            throw new AcademicRuleException("INVALID_ENROLLMENT",
                    "Thời gian gỡ ghi danh không hợp lệ");
        }
    }

    /**
     * Factory method creating a new active enrollment record.
     */
    public static Enrollment createEnrollment(UUID classId, UUID accountId, EnrollmentSource source,
            Instant now) {
        return new Enrollment(classId, accountId, EnrollmentStatus.ACTIVE, source, now, null);
    }

    /**
     * Marks an active enrollment as removed with the current timestamp.
     */
    public Enrollment removeEnrollment(Instant now) {
        return status == EnrollmentStatus.REMOVED ? this
                : new Enrollment(classId, accountId, EnrollmentStatus.REMOVED, source, enrolledAt,
                        Objects.requireNonNull(now));
    }

    /**
     * Restores a previously removed enrollment back to active status with a new enrollment source.
     */
    public Enrollment restoreEnrollment(EnrollmentSource source, Instant now) {
        if (status == EnrollmentStatus.ACTIVE)
            throw new AcademicRuleException("ALREADY_ENROLLED", "Sinh viên đã được ghi danh");
        if (Objects.requireNonNull(now).isBefore(removedAt)) {
            throw new AcademicRuleException("INVALID_ENROLLMENT_TIME",
                    "Thời gian khôi phục không hợp lệ");
        }
        return createEnrollment(classId, accountId, source, now);
    }
}
