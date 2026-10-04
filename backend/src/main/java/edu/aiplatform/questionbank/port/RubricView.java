package edu.aiplatform.questionbank.port;

import java.math.BigDecimal;
import java.util.UUID;

public record RubricView(UUID rubricId, UUID lineageId, int version, String title, RubricCriteria criteria, BigDecimal totalPoints) {}
