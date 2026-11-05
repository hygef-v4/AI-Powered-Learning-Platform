package edu.aiplatform.grading.model;

import static org.assertj.core.api.Assertions.*;

import edu.aiplatform.assignments.contract.*;
import edu.aiplatform.shared.error.ApiException;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class RubricScoringTest {
    final UUID rubricId = UUID.randomUUID(), itemId = UUID.randomUUID();
    final List<RubricView> rubrics = List.of(new RubricView(rubricId, UUID.randomUUID(), "p",
            List.of(new RubricCriterion(UUID.randomUUID(), "Criterion", "", CriterionKind.TEXT,
                    List.of(new RubricItem(itemId, "Answer", new BigDecimal("2.50"))))), new BigDecimal("2.50")));
    final Map<UUID, Set<UUID>> checks = Map.of(rubricId, Set.of(itemId));

    @Test void explicitZeroAndPartialScoresOverrideChecks() {
        assertThat(RubricScoring.calculateScore(rubrics, checks)).isEqualByComparingTo("2.50");
        for (var points : List.of(BigDecimal.ZERO, new BigDecimal("1.25")))
            assertThat(RubricScoring.calculateScore(rubrics, checks, Map.of(rubricId, Map.of(itemId, points))))
                    .isEqualByComparingTo(points);
        assertThat(RubricScoring.calculateScore(rubrics, Map.of(), null)).isEqualByComparingTo("0");
    }

    @Test void rejectsInvalidPointsInsteadOfRoundingInput() {
        for (String points : List.of("-0.01", "2.51", "1.255"))
            assertThatThrownBy(() -> RubricScoring.calculateScore(rubrics, checks,
                    Map.of(rubricId, Map.of(itemId, new BigDecimal(points)))))
                    .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("INVALID_RUBRIC_SCORE"));
        var nullItem = new HashMap<UUID, BigDecimal>();
        nullItem.put(itemId, null);
        assertThatThrownBy(() -> RubricScoring.calculateScore(rubrics, checks, Map.of(rubricId, nullItem)))
                .isInstanceOf(ApiException.class);
    }

    @Test void rejectsUnknownRubricsItemsAndNullChecks() {
        assertThatThrownBy(() -> RubricScoring.calculateScore(rubrics, null)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> RubricScoring.calculateScore(rubrics, Map.of(UUID.randomUUID(), Set.of())))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("INVALID_RUBRIC"));
        assertThatThrownBy(() -> RubricScoring.calculateScore(rubrics, Map.of(rubricId, Set.of(UUID.randomUUID()))))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> RubricScoring.calculateScore(rubrics, checks, Map.of(rubricId, Map.of(UUID.randomUUID(), BigDecimal.ONE))))
                .isInstanceOf(ApiException.class);
    }

    @Test void effectiveScoresRetainLegacyNullAndHalfUpRounding() {
        assertThat(RubricScoring.calculateEffectiveItemScores(null, null, null)).isEmpty();
        assertThat(RubricScoring.calculateEffectiveItemScores(rubrics, null, null).get(rubricId).get(itemId))
                .isEqualByComparingTo("0.00");
        assertThat(RubricScoring.calculateEffectiveItemScores(rubrics, checks,
                Map.of(rubricId, Map.of(itemId, new BigDecimal("1.255")))).get(rubricId).get(itemId))
                .isEqualByComparingTo("1.26");
    }

    @Test void copiesNormalizeNullMapsAndDoNotShareMutableCollections() {
        var items = new HashSet<>(Set.of(itemId));
        var scores = new HashMap<UUID, BigDecimal>(Map.of(itemId, BigDecimal.ONE));
        var copiedChecks = RubricScoring.copyChecks(Map.of(rubricId, items));
        var copiedScores = RubricScoring.copyItemScores(Map.of(rubricId, scores));
        items.clear(); scores.clear();
        assertThat(copiedChecks.get(rubricId)).containsExactly(itemId);
        assertThat(copiedScores.get(rubricId)).containsEntry(itemId, BigDecimal.ONE);
        assertThat(RubricScoring.copyChecks(null)).isEmpty();
        assertThat(RubricScoring.copyItemScores(null)).isEmpty();
    }
}
