package edu.aiplatform.quizzes.contract;

import java.util.Set;
import java.util.UUID;

public record AnswerKey(UUID questionId, Set<String> correctOptionIds, String explanation) {
}
