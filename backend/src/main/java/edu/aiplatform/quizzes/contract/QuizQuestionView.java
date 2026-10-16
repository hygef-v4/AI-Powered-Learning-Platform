package edu.aiplatform.quizzes.contract;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Câu hỏi cho người học, không có đáp án. */
public record QuizQuestionView(UUID questionId, QuestionType type, String stem, List<QuizOptionView> options,
                               BigDecimal points) {
}
