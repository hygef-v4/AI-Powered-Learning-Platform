package edu.aiplatform.attempts.application;

import edu.aiplatform.attempts.port.*;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Khung cài đặt của U11: thân hàm tạm trả giá trị rỗng/an toàn hoặc báo chưa cài.
 */
@Service
public class DraftSaver implements AttemptRunResultPort {

    @Override
    public void recordRunResult(UUID attemptId, String runResultJson) {
        // Chưa cài: U11.
    }
}
