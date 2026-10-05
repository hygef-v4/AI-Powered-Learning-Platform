package edu.aiplatform.attempts.port;

import java.util.UUID;

/** U13 ghi kết quả chạy thử/chấm test mới nhất vào `attempts.run_result`. */
public interface AttemptRunResultPort {
    void recordRunResult(UUID attemptId, String runResultJson);
}
