package edu.aiplatform.quizzes.model;

import java.math.BigDecimal;
import java.util.UUID;

/** Một quy tắc sinh đề (BR-U10-17): lấy {@code questionCount} câu mang {@code tag}, mỗi câu {@code pointsEach} điểm. */
public record DrawRule(UUID quizId, int position, String tag, Difficulty difficulty, int questionCount,
                       BigDecimal pointsEach) {
}
