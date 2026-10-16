package edu.aiplatform.quizzes.contract;

import java.math.BigDecimal;
import java.util.List;

public record QuizScore(List<QuestionScore> questions, BigDecimal total, BigDecimal maxTotal) {
}
