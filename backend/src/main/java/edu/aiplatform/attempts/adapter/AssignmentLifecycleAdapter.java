package edu.aiplatform.attempts.adapter;

import edu.aiplatform.assessments.port.*;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Khung cài đặt của U11: thân hàm tạm trả giá trị rỗng/an toàn hoặc báo chưa cài.
 * Plan U11: Ngưng giao: tự nộp lượt đang làm.
 */
@Component("attemptsAssignmentLifecycleAdapter")
public class AssignmentLifecycleAdapter implements AssignmentLifecyclePort {

    @Override
    public void onOpened(UUID assignmentId) {
        // Chưa cài: U11.
    }

    @Override
    public void onRetired(UUID assignmentId) {
        // Chưa cài: U11.
    }
}
