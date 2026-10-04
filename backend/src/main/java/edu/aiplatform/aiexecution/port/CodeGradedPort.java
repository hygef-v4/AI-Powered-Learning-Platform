package edu.aiplatform.aiexecution.port;

import java.util.UUID;

/** U13 khai báo, U15 cài (`C`): điểm Code Lab bài GRADED. */
public interface CodeGradedPort {
    void onGraded(UUID attemptId, CodeRunResult result);
}
