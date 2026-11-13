package edu.aiplatform.academics.model;

import edu.aiplatform.academics.contract.ClassStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class EnrollmentTest {
    private final UUID classId = UUID.randomUUID();
    private final UUID student = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-10-17T00:00:00Z");

    @Test void removesAndRestoresSameIdentityWithNewEnrollmentDate() {
        var first = Enrollment.createEnrollment(classId, student, EnrollmentSource.MANUAL, now);
        var removed = first.removeEnrollment(now.plusSeconds(1));
        var restored = removed.restoreEnrollment(EnrollmentSource.LIST, now.plusSeconds(2));
        assertThat(removed.status()).isEqualTo(EnrollmentStatus.REMOVED);
        assertThat(removed.removedAt()).isEqualTo(now.plusSeconds(1));
        assertThat(restored.classId()).isEqualTo(classId);
        assertThat(restored.accountId()).isEqualTo(student);
        assertThat(restored.status()).isEqualTo(EnrollmentStatus.ACTIVE);
        assertThat(restored.removedAt()).isNull();
        assertThat(restored.enrolledAt()).isEqualTo(now.plusSeconds(2));
        assertThat(restored.source()).isEqualTo(EnrollmentSource.LIST);
        assertThat(first.status()).isEqualTo(EnrollmentStatus.ACTIVE);
    }

    @Test void removalIsIdempotentAndRestoringActiveIsRejected() {
        var value = Enrollment.createEnrollment(classId, student, EnrollmentSource.MANUAL, now);
        var removed = value.removeEnrollment(now);
        assertThat(removed.removeEnrollment(now.plusSeconds(2))).isEqualTo(removed);
        assertThatThrownBy(() -> value.restoreEnrollment(EnrollmentSource.LIST, now)).isInstanceOf(AcademicRuleException.class);
        assertThatThrownBy(() -> value.removeEnrollment(now.minusSeconds(1))).isInstanceOf(AcademicRuleException.class);
    }

    @Test void policyDistinguishesFirstEnrollmentRestoreDuplicateAndOtherClass() {
        assertThat(EnrollmentPolicy.resolveEnrollmentResult(ClassStatus.DRAFT, null, false)).isEqualTo(EnrollmentResult.ENROLLED);
        var active = Enrollment.createEnrollment(classId, student, EnrollmentSource.MANUAL, now);
        assertThat(EnrollmentPolicy.resolveEnrollmentResult(ClassStatus.OPEN, active, false)).isEqualTo(EnrollmentResult.ALREADY_ENROLLED);
        assertThat(EnrollmentPolicy.resolveEnrollmentResult(ClassStatus.OPEN, active.removeEnrollment(now), false)).isEqualTo(EnrollmentResult.RESTORED);
        assertThat(EnrollmentPolicy.resolveEnrollmentResult(ClassStatus.OPEN, null, true)).isEqualTo(EnrollmentResult.IN_OTHER_CLASS);
        assertThatThrownBy(() -> EnrollmentPolicy.resolveEnrollmentResult(ClassStatus.ARCHIVED, null, false))
                .isInstanceOf(AcademicRuleException.class);
    }
}
