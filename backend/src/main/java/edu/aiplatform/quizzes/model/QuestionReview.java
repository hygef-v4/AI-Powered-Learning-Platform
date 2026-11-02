package edu.aiplatform.quizzes.model;

import java.time.Instant;
import java.util.UUID;

/** Một lần Chủ nhiệm môn duyệt hoặc từ chối câu đề xuất (BR-U10-09); {@code decision}: {@code APPROVED}, {@code REJECTED}. */
public record QuestionReview(UUID id, UUID questionId, String decision, String reason, UUID reviewerId, Instant createdAt) {
}
