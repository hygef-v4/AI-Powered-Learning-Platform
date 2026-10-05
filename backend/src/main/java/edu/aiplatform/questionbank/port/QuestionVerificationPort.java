package edu.aiplatform.questionbank.port;

import java.util.UUID;

/** U13 ghi kết quả kiểm lời giải mẫu vào `questions.definition.verification`. */
public interface QuestionVerificationPort {
    void record(UUID questionId, String contentHash, boolean allPassed, String resultJson);
}
