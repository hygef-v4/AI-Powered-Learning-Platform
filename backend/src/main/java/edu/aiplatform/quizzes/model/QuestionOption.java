package edu.aiplatform.quizzes.model;

/** Một lựa chọn của câu trắc nghiệm; {@code id} do người soạn đặt (ví dụ "1".."6"), duy nhất trong câu. */
public record QuestionOption(String id, String text) {
}
