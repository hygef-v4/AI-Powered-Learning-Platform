package edu.aiplatform.academics.domain;

import edu.aiplatform.academics.contract.ClassStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.*;

class CourseClassTest {
    private final Instant now = Instant.parse("2026-10-17T00:00:00Z");
    private final UUID teacher = UUID.randomUUID();

    private CourseClass draft() {
        return CourseClass.create(UUID.randomUUID(), UUID.randomUUID(), " SE01 ", " Lớp 1 ", null, "2026-1", teacher, now);
    }

    @Test void createsDraftWithDistributionDisabled() {
        var value = draft();
        assertThat(value.status()).isEqualTo(ClassStatus.DRAFT);
        assertThat(value.code()).isEqualTo("SE01");
        assertThat(value.name()).isEqualTo("Lớp 1");
        assertThat(value.showGradeDistribution()).isFalse();
        assertThat(value.version()).isZero();
    }

    @ParameterizedTest
    @CsvSource({"DRAFT,OPEN", "DRAFT,ARCHIVED", "OPEN,ARCHIVED", "ARCHIVED,OPEN"})
    void allowsOnlySpecifiedTransitions(ClassStatus from, ClassStatus to) {
        var base = draft();
        var value = new CourseClass(base.id(), base.subjectId(), base.code(), base.name(), null, base.term(),
                from, teacher, false, now, now, 7);
        var changed = value.changeStatus(to, true, false, now.plusSeconds(1));
        assertThat(changed.status()).isEqualTo(to);
        assertThat(changed.version()).isEqualTo(8);
        assertThat(value.status()).isEqualTo(from);
    }

    @ParameterizedTest
    @CsvSource({"DRAFT,DRAFT", "OPEN,DRAFT", "OPEN,OPEN", "ARCHIVED,DRAFT", "ARCHIVED,ARCHIVED"})
    void rejectsOtherTransitions(ClassStatus from, ClassStatus to) {
        var base = draft();
        var value = new CourseClass(base.id(), base.subjectId(), base.code(), base.name(), null, base.term(),
                from, teacher, false, now, now, 0);
        assertThatThrownBy(() -> value.changeStatus(to, true, false, now)).isInstanceOf(AcademicRuleException.class)
                .hasMessageContaining("trạng thái");
    }

    @Test void openingRequiresTeacherAndActiveSubject() {
        var noTeacher = CourseClass.create(UUID.randomUUID(), UUID.randomUUID(), "C", "Class", null, null, null, now);
        assertThatThrownBy(() -> noTeacher.changeStatus(ClassStatus.OPEN, true, false, now))
                .isInstanceOf(AcademicRuleException.class).hasMessageContaining("giảng viên");
        assertThatThrownBy(() -> draft().changeStatus(ClassStatus.OPEN, false, false, now))
                .isInstanceOf(AcademicRuleException.class).hasMessageContaining("môn");
    }

    @Test void reopeningRejectsConflictingEnrollments() {
        var archived = draft().changeStatus(ClassStatus.ARCHIVED, true, false, now);
        assertThatThrownBy(() -> archived.changeStatus(ClassStatus.OPEN, true, true, now))
                .isInstanceOf(AcademicRuleException.class).hasMessageContaining("lớp khác");
    }

    @Test void archivedIsReadOnlyAndOpenTeacherCannotBeCleared() {
        var archived = draft().changeStatus(ClassStatus.ARCHIVED, true, false, now);
        assertThatThrownBy(() -> archived.updateInfo("New", null, null, now)).isInstanceOf(AcademicRuleException.class);
        assertThatThrownBy(() -> archived.assignTeacher(teacher, now)).isInstanceOf(AcademicRuleException.class);
        assertThatThrownBy(() -> archived.setGradeDistribution(true, now)).isInstanceOf(AcademicRuleException.class);
        var open = draft().changeStatus(ClassStatus.OPEN, true, false, now);
        assertThatThrownBy(() -> open.assignTeacher(null, now)).isInstanceOf(AcademicRuleException.class);
    }

    @Test void updatesPreserveIdentityAndAdvanceVersion() {
        var before = draft();
        var after = before.updateInfo(" Updated ", "Description", "2026-2", now.plusSeconds(1));
        assertThat(after.id()).isEqualTo(before.id());
        assertThat(after.subjectId()).isEqualTo(before.subjectId());
        assertThat(after.code()).isEqualTo(before.code());
        assertThat(after.name()).isEqualTo("Updated");
        assertThat(after.version()).isEqualTo(1);
        assertThat(after.updatedAt()).isEqualTo(now.plusSeconds(1));
        assertThat(before.assignTeacher(null, now).teacherId()).isNull();
        assertThat(before.setGradeDistribution(true, now).showGradeDistribution()).isTrue();
    }

    @Test void rejectsInvalidTextAndTimestamps() {
        assertThatThrownBy(() -> draft().updateInfo(" ", null, null, now)).isInstanceOf(AcademicRuleException.class);
        assertThatThrownBy(() -> draft().updateInfo("x".repeat(201), null, null, now)).isInstanceOf(AcademicRuleException.class);
        assertThatThrownBy(() -> draft().updateInfo("x", "x".repeat(2001), null, now)).isInstanceOf(AcademicRuleException.class);
        assertThatThrownBy(() -> draft().updateInfo("x", null, "x".repeat(21), now)).isInstanceOf(AcademicRuleException.class);
        assertThatThrownBy(() -> draft().updateInfo("x", null, null, now.minusSeconds(1))).isInstanceOf(AcademicRuleException.class);
        assertThatThrownBy(() -> CourseClass.create(UUID.randomUUID(), UUID.randomUUID(), "x".repeat(31), "X", null, null, teacher, now))
                .isInstanceOf(AcademicRuleException.class);
    }
}
