package edu.aiplatform.quizzes.model;

/**
 * Trạng thái một phiên bản câu (BR-U10-04, 05, 09). Câu riêng của Quiz chỉ có {@code DRAFT}. {@code PENDING} và
 * {@code REJECTED} là câu giảng viên đề xuất, chờ duyệt hoặc đã bị từ chối.
 */
public enum QuestionStatus {
    DRAFT, PENDING, REJECTED, ACTIVE, RETIRED
}
