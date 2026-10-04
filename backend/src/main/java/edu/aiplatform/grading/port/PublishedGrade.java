package edu.aiplatform.grading.port;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PublishedGrade(UUID evaluationId, UUID assignmentId, UUID accountId, BigDecimal score, BigDecimal maxScore, String feedback, Instant publishedAt, boolean updated) {}
