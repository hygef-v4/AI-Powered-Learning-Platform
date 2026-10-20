package edu.aiplatform.quizzes.domain;

import java.math.BigDecimal;
import java.util.UUID;

/** Một câu trong Quiz: ghim id phiên bản câu (BR-U10-04, 13), thứ tự từ 1 và điểm của câu trong Quiz này. */
public record QuizItem(UUID quizId, UUID questionId, int position, BigDecimal points) {
}
