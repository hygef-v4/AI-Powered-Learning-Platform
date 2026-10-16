package edu.aiplatform.quizzes.contract;

import java.util.List;
import java.util.UUID;

public record AnswerReview(UUID quizId, List<AnswerKey> answers) {
}
