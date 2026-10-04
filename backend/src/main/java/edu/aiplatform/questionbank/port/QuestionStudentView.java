package edu.aiplatform.questionbank.port;

import java.util.UUID;

/** Đã bỏ đáp án, answerGuide, test ẩn (P6). */
public record QuestionStudentView(UUID id, QuestionType questionType, String studentDefinitionJson) {}
