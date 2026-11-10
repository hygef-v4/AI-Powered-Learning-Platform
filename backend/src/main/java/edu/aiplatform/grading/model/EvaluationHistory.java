package edu.aiplatform.grading.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record EvaluationHistory(UUID actorId, Instant at, BigDecimal before, BigDecimal after,
        String reason, String previousChecks, String previousFeedback,
        Map<UUID, Map<UUID, BigDecimal>> previousItemScores,
        GradingFeedback previousStructuredFeedback) {
    /** Creates a history entry with no detailed rubric snapshot. */
    public EvaluationHistory(UUID actorId, Instant at, BigDecimal before, BigDecimal after,
            String reason, String previousChecks, String previousFeedback,
            Map<UUID, Map<UUID, BigDecimal>> previousItemScores) {
        this(actorId, at, before, after, reason, previousChecks, previousFeedback,
                previousItemScores, null);
    }

    /** Creates a history entry with rubric scores and structured feedback snapshots. */
    public EvaluationHistory(UUID actorId, Instant at, BigDecimal before, BigDecimal after,
            String reason, String previousChecks, String previousFeedback) {
        this(actorId, at, before, after, reason, previousChecks, previousFeedback, Map.of(), null);
    }
}
