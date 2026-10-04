package edu.aiplatform.grading.port;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record GradebookRow(UUID accountId, UUID assignmentId, BigDecimal score, BigDecimal maxScore, String status, Instant submittedAt, String feedback) {}
