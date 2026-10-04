package edu.aiplatform.groups.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** U12 cung cấp cho U14, U16. */
public interface GroupMembershipPort {
    Optional<GroupView> groupOf(UUID studentId, UUID classId);
    List<GroupView> groupsOf(UUID classId);
    List<UUID> members(UUID groupId);
    UUID leaderOf(UUID groupId);
}
