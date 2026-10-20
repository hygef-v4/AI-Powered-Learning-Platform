package edu.aiplatform.grading.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record EvaluationHistory(UUID actorId, Instant at, BigDecimal before, BigDecimal after, String reason,
        String previousChecks, String previousFeedback) {
}
