package edu.aiplatform.attempts.port;

import java.util.UUID;

/** U11 khai báo, U15 cài (`C`): chỉ bài GRADED, gọi trong transaction nộp. */
public interface SubmissionSubmittedPort {
    void onSubmitted(UUID attemptId);
}
