package edu.aiplatform.grading.model;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EditableGradeTest {
    private final UUID teacher = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-11-11T00:00:00Z");

    private Evaluation createGrade() {
        var grade = Evaluation.createPendingEvaluation(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), "hash", BigDecimal.TEN, "[]", now);
        grade.saveGrade("{}", BigDecimal.ONE, BigDecimal.ZERO, null, "Draft", null, teacher, now);
        return grade;
    }

    @Test
    void draftsPublishDirectlyAndCorrectionsStayPublished() {
        var grade = createGrade();
        grade.publishGrade(teacher, now);
        grade.saveGrade("{}", BigDecimal.TEN, BigDecimal.ZERO, null, "Corrected", null,
                teacher, now.plusSeconds(30));
        assertThat(grade.state).isEqualTo(EvaluationStatus.PUBLISHED);
        assertThat(grade.publishedAt).isEqualTo(now);
        assertThat(grade.score).isEqualByComparingTo("10");
        assertThat(grade.feedback).isEqualTo("Corrected");
        assertThat(grade.changes).hasSize(1);
    }

    @Test
    void legacyFinalizationDoesNotLockOrCreateAFinalizedGrade() {
        var grade = createGrade();
        grade.finalizeGrade(teacher, now);
        assertThat(grade.state).isEqualTo(EvaluationStatus.DRAFT);
        grade.state = EvaluationStatus.FINALIZED;
        grade.saveGrade("{}", BigDecimal.TEN, BigDecimal.ZERO, null, "Correction", null,
                teacher, now.plusSeconds(1));
        assertThat(grade.state).isEqualTo(EvaluationStatus.DRAFT);
        grade.publishGrade(teacher, now.plusSeconds(2));
        var publishedAt = grade.publishedAt;
        grade.publishGrade(teacher, now.plusSeconds(3));
        assertThat(grade.publishedAt).isEqualTo(publishedAt);
    }
}
