package edu.aiplatform.attempts.port;

import java.util.UUID;

/** U11, U13 khai báo, U15 cài (`C`): ghi `evaluations` `kind = PRACTICE`, `PUBLISHED`. */
public interface PracticeResultPort {
    void scoreQuiz(UUID attemptId);
    void record(UUID attemptId, PracticeResult result);
}
