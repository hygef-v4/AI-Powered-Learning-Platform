package edu.aiplatform.aiexecution.port;

import edu.aiplatform.shared.contract.ActorRef;
import java.util.Map;
import java.util.UUID;

/** U13 cung cấp cho U11, U15; chạy trong Judge0 (BR-U13-31…37). */
public interface CodeRunPort {
    /** Đồng bộ, chỉ test công khai, 5 lần/phút. */
    CodeRunResult tryRun(ActorRef student, UUID attemptId, Map<String, String> files);
    /** Qua việc `CODE_RUN`; kết quả về U15 (`CodeGradedPort`) hoặc `PracticeResultPort`. */
    void grade(UUID attemptId);
}
