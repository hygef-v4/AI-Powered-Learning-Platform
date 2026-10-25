package edu.aiplatform.grading.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.Map;

public record EvaluationHistory(UUID actorId, Instant at, BigDecimal before, BigDecimal after, String reason,
        String previousChecks, String previousFeedback, Map<UUID, Map<UUID, BigDecimal>> previousItemScores) {
    public EvaluationHistory(UUID actorId, Instant at, BigDecimal before, BigDecimal after, String reason,
            String previousChecks, String previousFeedback) {
        this(actorId, at, before, after, reason, previousChecks, previousFeedback, Map.of());
    }
}
