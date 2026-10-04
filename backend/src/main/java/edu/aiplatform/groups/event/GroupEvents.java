package edu.aiplatform.groups.event;

import java.util.List;
import java.util.UUID;

public final class GroupEvents {
    public record MembershipChanged(UUID groupId, UUID classId, List<UUID> addedIds, List<UUID> removedIds) {}

    public record LeaderChanged(UUID groupId, UUID classId, UUID newLeaderId) {}

    public record LeaderRequested(UUID requestId, UUID groupId, UUID classId, UUID requesterId) {}

    public record LeaderRequestRejected(UUID requestId, UUID groupId, UUID requesterId, String decisionReason) {}

    private GroupEvents() {}
}
