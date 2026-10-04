package edu.aiplatform.aiexecution.application;

import edu.aiplatform.aiexecution.port.*;
import edu.aiplatform.shared.contract.ActorRef;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Khung cài đặt của U13: thân hàm tạm trả giá trị rỗng/an toàn hoặc báo chưa cài.
 */
@Service
public class CodeRunService implements CodeRunPort {

    @Override
    public CodeRunResult tryRun(ActorRef student, UUID attemptId, Map<String, String> files) {
        throw new UnsupportedOperationException("Chưa cài: U13");
    }

    @Override
    public void grade(UUID attemptId) {
        // Chưa cài: U13.
    }
}
