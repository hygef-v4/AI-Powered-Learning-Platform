package edu.aiplatform.groupdocs.adapter;

import edu.aiplatform.groups.port.*;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Khung cài đặt của U14: thân hàm tạm trả giá trị rỗng/an toàn hoặc báo chưa cài.
 */
@Component
public class GroupChangeAdapter implements GroupChangePort {

    @Override
    public void onGroupCreated(UUID groupId) {
        // Chưa cài: U14.
    }

    @Override
    public void onMemberRemoved(UUID groupId, UUID studentId) {
        // Chưa cài: U14.
    }

    @Override
    public boolean hasGroupWork(UUID groupId) {
        return false;
    }
}
