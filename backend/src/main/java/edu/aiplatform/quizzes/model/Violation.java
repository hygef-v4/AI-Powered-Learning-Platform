package edu.aiplatform.quizzes.model;

/** Một lỗi nội dung câu: trường, mã máy đọc được và thông điệp cho người soạn. */
public record Violation(String field, String code, String message) {
}
