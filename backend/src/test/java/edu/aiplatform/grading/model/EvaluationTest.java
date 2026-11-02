package edu.aiplatform.grading.model;

import static org.assertj.core.api.Assertions.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class EvaluationTest {
    private final UUID actor = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-10-20T00:00:00Z");

    private Evaluation evaluation() {
        return Evaluation.pending(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "hash",
                new BigDecimal("10"), "[]", now);
    }

    @Test
    void pendingCannotBeFinalized() {
        assertThatThrownBy(() -> evaluation().finalizeGrade(actor, now)).hasMessageContaining("DRAFT");
    }

    @Test
    void saveClampsScoreAndRequiresAdjustmentReason() {
        var e = evaluation();
        assertThatThrownBy(() -> e.save("{}", new BigDecimal("4"), new BigDecimal("-1"), "", null, null, actor, now))
                .hasMessageContaining("reason");
        e.save("{}", new BigDecimal("4"), new BigDecimal("-1"), "late", "feedback", null, actor, now);
        assertThat(e.score()).isEqualByComparingTo("3.00");
        assertThat(e.status()).isEqualTo(EvaluationStatus.DRAFT);
        e.save("{}", new BigDecimal("4"), new BigDecimal("100"), "bonus", null, null, actor, now);
        assertThat(e.score()).isEqualByComparingTo("10.00");
    }

    @Test
    void publishedChangeKeepsStatusAndHistoryAndNeedsReason() {
        var e = evaluation();
        e.save("{}", new BigDecimal("4"), BigDecimal.ZERO, null, "ok", null, actor, now);
        e.finalizeGrade(actor, now);
        e.publish(actor, now);
        assertThatThrownBy(() -> e.save("{}", new BigDecimal("6"), BigDecimal.ZERO, null, null, null, actor, now))
                .hasMessageContaining("reason");
        e.save("{}", new BigDecimal("6"), BigDecimal.ZERO, null, "updated", "correction", actor, now);
        assertThat(e.status()).isEqualTo(EvaluationStatus.PUBLISHED);
        assertThat(e.history()).hasSize(1);
        assertThat(e.history().get(0).before()).isEqualByComparingTo("4");
    }

    @Test
    void publishedOldSubmissionIsRetainedButUnpublishedIsSuperseded() {
        var e = evaluation();
        e.supersede();
        assertThat(e.status()).isEqualTo(EvaluationStatus.SUPERSEDED);
        assertThatThrownBy(() -> e.save("{}", BigDecimal.ONE, BigDecimal.ZERO, null, null, null, actor, now))
                .hasMessageContaining("superseded");
        var p = evaluation();
        p.save("{}", BigDecimal.ONE, BigDecimal.ZERO, null, null, null, actor, now);
        p.finalizeGrade(actor, now);
        p.publish(actor, now);
        p.supersede();
        assertThat(p.status()).isEqualTo(EvaluationStatus.PUBLISHED);
    }

    @Test
    void feedbackLimitAndPublishRequireValidState() {
        var e = evaluation();
        assertThatThrownBy(() -> e.publish(actor, now)).hasMessageContaining("FINALIZED");
        assertThatThrownBy(
                () -> e.save("{}", BigDecimal.ONE, BigDecimal.ZERO, null, "x".repeat(10001), null, actor, now))
                .hasMessageContaining("10000");
    }
}
