package edu.aiplatform.quizpractice.domain;

import java.math.BigDecimal;
import java.util.UUID;

/** Kết quả một câu của lượt đã nộp, chép từ {@code IQuizScoring.score} của U10. */
public record QuestionResult(UUID questionId, boolean correct, BigDecimal points, BigDecimal maxPoints) {
}
