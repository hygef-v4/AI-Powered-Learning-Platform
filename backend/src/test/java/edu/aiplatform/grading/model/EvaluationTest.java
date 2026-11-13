package edu.aiplatform.grading.model;

import static org.assertj.core.api.Assertions.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class EvaluationTest {
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"-0.25,3.75", "0.25,4.25"})
    void adjustmentsCanUseAnEmptyOptionalNote(String adjustment, String expected) {
        var grade = evaluation();
        grade.saveGrade("{}", new BigDecimal("4"), new BigDecimal(adjustment), "", null,
                null, actor, now);
        assertThat(grade.score()).isEqualByComparingTo(expected);
    }
    private final UUID actor = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-10-20T00:00:00Z");

    private Evaluation evaluation() {
        return Evaluation.createPendingEvaluation(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "hash",
                new BigDecimal("10"), "[]", now);
    }

    @Test
    void pendingCannotBeFinalized() {
        assertThatThrownBy(() -> evaluation().finalizeGrade(actor, now)).hasMessageContaining("DRAFT");
    }

    @Test
    void saveClampsScoreAndAllowsOptionalAdjustmentReason() {
        var e = evaluation();
        e.saveGrade("{}", new BigDecimal("4"), new BigDecimal("-1"), null, null, null, actor, now);
        assertThat(e.score()).isEqualByComparingTo("3.00");
        e.saveGrade("{}", new BigDecimal("4"), new BigDecimal("-1"), "late", "feedback", null, actor, now);
        assertThat(e.score()).isEqualByComparingTo("3.00");
        assertThat(e.status()).isEqualTo(EvaluationStatus.DRAFT);
        e.saveGrade("{}", new BigDecimal("4"), new BigDecimal("100"), "bonus", null, null, actor, now);
        assertThat(e.score()).isEqualByComparingTo("10.00");
    }

    @Test
    void legacyFinalizedAndPublishedGradesAllowCorrections() {
        var e = evaluation();
        e.saveGrade("{}", new BigDecimal("4"), BigDecimal.ZERO, null, "ok", null, actor, now);
        e.state = EvaluationStatus.FINALIZED;
        e.saveGrade("{}", new BigDecimal("6"), BigDecimal.ZERO, null, "updated", "correction", actor, now);
        assertThat(e.state).isEqualTo(EvaluationStatus.DRAFT);
        e.publishGrade(actor, now);
        e.overrideMemberScore(BigDecimal.TEN, BigDecimal.ZERO, "correction", actor, now.plusSeconds(1));
        assertThat(e.memberOverride).isTrue();
        assertThat(e.state).isEqualTo(EvaluationStatus.PUBLISHED);
        assertThat(e.score).isEqualByComparingTo("10");
        assertThat(e.history()).hasSize(2);
    }

    @Test
    void resubmissionSupersedesOldGradeIncludingPublished() {
        var e = evaluation();
        e.supersede();
        assertThat(e.status()).isEqualTo(EvaluationStatus.SUPERSEDED);
        assertThatThrownBy(() -> e.saveGrade("{}", BigDecimal.ONE, BigDecimal.ZERO, null, null, null, actor, now))
                .hasMessageContaining("superseded");
        var p = evaluation();
        p.saveGrade("{}", BigDecimal.ONE, BigDecimal.ZERO, null, null, null, actor, now);
        p.finalizeGrade(actor, now);
        p.publishGrade(actor, now);
        p.supersede();
        assertThat(p.status()).isEqualTo(EvaluationStatus.SUPERSEDED);
    }

    @Test
    void feedbackLimitAndPublishRequireValidState() {
        var e = evaluation();
        assertThatThrownBy(() -> e.publishGrade(actor, now)).hasMessageContaining("DRAFT");
        assertThatThrownBy(
                () -> e.saveGrade("{}", BigDecimal.ONE, BigDecimal.ZERO, null, "x".repeat(10001), null, actor, now))
                .hasMessageContaining("10000");
    }
    @Test
    void draftScoreChangesKeepHistoryAndGroupAdjustment() {
        var root = evaluation();
        root.saveGrade("{}", new BigDecimal("6"), BigDecimal.ONE, "bonus", "old", null, actor, now);
        root.saveGrade("{}", new BigDecimal("7"), BigDecimal.ONE, "bonus", "new", "correction", actor, now.plusSeconds(1));
        assertThat(root.history()).singleElement().satisfies(h -> {
            assertThat(h.before()).isEqualByComparingTo("7");
            assertThat(h.after()).isEqualByComparingTo("8");
            assertThat(h.previousFeedback()).isEqualTo("old");
            assertThat(h.reason()).isEqualTo("correction");
        });
        var member = evaluation();
        member.inheritGroupGrade(root, null, actor, now);
        assertThat(member.score).isEqualByComparingTo("8");
        assertThat(member.adjustment).isEqualByComparingTo("1");
        member.overrideMemberScore(new BigDecimal("5"), new BigDecimal("7"), "contribution", actor, now);
        assertThat(member.adjustment).isEqualByComparingTo("-2");
        root.saveGrade("{}", new BigDecimal("8"), BigDecimal.ONE, "bonus", "new", null, actor, now);
        member.inheritGroupGrade(root, null, actor, now);
        assertThat(member.score).isEqualByComparingTo("5");
        assertThat(member.adjustment).isEqualByComparingTo("-3");
    }
}
