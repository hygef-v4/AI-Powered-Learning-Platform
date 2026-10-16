package edu.aiplatform.assignments.contract;

import java.math.BigDecimal;
import java.util.UUID;

public record RubricItem(UUID itemId, String text, BigDecimal points) {
}
