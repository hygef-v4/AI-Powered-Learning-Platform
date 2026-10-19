package edu.aiplatform.academics.domain;

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
        var first = Enrollment.create(classId, student, EnrollmentSource.MANUAL, now);
        var removed = first.remove(now.plusSeconds(1));
        var restored = removed.restore(EnrollmentSource.LIST, now.plusSeconds(2));
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
        var value = Enrollment.create(classId, student, EnrollmentSource.MANUAL, now);
        var removed = value.remove(now);
        assertThat(removed.remove(now.plusSeconds(2))).isEqualTo(removed);
        assertThatThrownBy(() -> value.restore(EnrollmentSource.LIST, now)).isInstanceOf(AcademicRuleException.class);
        assertThatThrownBy(() -> value.remove(now.minusSeconds(1))).isInstanceOf(AcademicRuleException.class);
    }

    @Test void policyDistinguishesFirstEnrollmentRestoreDuplicateAndOtherClass() {
        assertThat(EnrollmentPolicy.resultFor(ClassStatus.DRAFT, null, false)).isEqualTo(EnrollmentResult.ENROLLED);
        var active = Enrollment.create(classId, student, EnrollmentSource.MANUAL, now);
        assertThat(EnrollmentPolicy.resultFor(ClassStatus.OPEN, active, false)).isEqualTo(EnrollmentResult.ALREADY_ENROLLED);
        assertThat(EnrollmentPolicy.resultFor(ClassStatus.OPEN, active.remove(now), false)).isEqualTo(EnrollmentResult.RESTORED);
        assertThat(EnrollmentPolicy.resultFor(ClassStatus.OPEN, null, true)).isEqualTo(EnrollmentResult.IN_OTHER_CLASS);
        assertThatThrownBy(() -> EnrollmentPolicy.resultFor(ClassStatus.ARCHIVED, null, false))
                .isInstanceOf(AcademicRuleException.class);
    }
}
