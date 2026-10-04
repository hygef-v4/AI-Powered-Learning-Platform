package edu.aiplatform.groupdocs.adapter;

import edu.aiplatform.assessments.port.*;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Khung cài đặt của U14: thân hàm tạm trả giá trị rỗng/an toàn hoặc báo chưa cài.
 * Plan U14: Mở bài: tạo tài liệu nhóm; ngưng giao: tự nộp.
 */
@Component("groupdocsAssignmentLifecycleAdapter")
public class AssignmentLifecycleAdapter implements AssignmentLifecyclePort {

    @Override
    public void onOpened(UUID assignmentId) {
        // Chưa cài: U14.
    }

    @Override
    public void onRetired(UUID assignmentId) {
        // Chưa cài: U14.
    }
}
