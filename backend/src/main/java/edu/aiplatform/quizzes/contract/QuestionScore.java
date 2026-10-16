package edu.aiplatform.quizzes.contract;

import java.math.BigDecimal;
import java.util.UUID;

public record QuestionScore(UUID questionId, boolean correct, BigDecimal points, BigDecimal maxPoints) {
}
