package edu.aiplatform.groups.application;

import edu.aiplatform.groups.port.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Khung cài đặt của U12: thân hàm tạm trả giá trị rỗng/an toàn hoặc báo chưa cài.
 * Plan U12: Bước 5.
 */
@Service
public class MembershipQueryService implements GroupMembershipPort {

    @Override
    public Optional<GroupView> groupOf(UUID studentId, UUID classId) {
        return Optional.empty();
    }

    @Override
    public List<GroupView> groupsOf(UUID classId) {
        return List.of();
    }

    @Override
    public List<UUID> members(UUID groupId) {
        return List.of();
    }

    @Override
    public UUID leaderOf(UUID groupId) {
        throw new UnsupportedOperationException("Chưa cài: U12");
    }
}
