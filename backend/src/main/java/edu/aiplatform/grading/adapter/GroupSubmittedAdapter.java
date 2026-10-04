package edu.aiplatform.grading.adapter;

import edu.aiplatform.groupdocs.port.*;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Khung cài đặt của U15: thân hàm tạm trả giá trị rỗng/an toàn hoặc báo chưa cài.
 * Plan U15: Bước 3.
 */
@Component
public class GroupSubmittedAdapter implements GroupSubmittedPort {

    @Override
    public void onGroupSubmitted(UUID groupDocumentId, List<UUID> memberIds) {
        // Chưa cài: U15.
    }
}
