package edu.aiplatform.assignments.contract;

import java.math.BigDecimal;
import java.util.UUID;

public record RubricScore(UUID rubricId, BigDecimal points, BigDecimal maxPoints) {
}
