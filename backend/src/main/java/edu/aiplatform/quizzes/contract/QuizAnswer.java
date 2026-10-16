package edu.aiplatform.quizzes.contract;

import java.util.Set;
import java.util.UUID;

public record QuizAnswer(UUID questionId, Set<String> selectedOptionIds) {
}
