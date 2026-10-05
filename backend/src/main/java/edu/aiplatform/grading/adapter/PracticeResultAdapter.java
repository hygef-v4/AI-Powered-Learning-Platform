package edu.aiplatform.grading.adapter;

import edu.aiplatform.attempts.port.*;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Khung cài đặt của U15: thân hàm tạm trả giá trị rỗng/an toàn hoặc báo chưa cài.
 * Plan U15: Bước 3.
 */
@Component
public class PracticeResultAdapter implements PracticeResultPort {

    @Override
    public void scoreQuiz(UUID attemptId) {
        // Chưa cài: U15.
    }

    @Override
    public void record(UUID attemptId, PracticeResult result) {
        // Chưa cài: U15.
    }
}
