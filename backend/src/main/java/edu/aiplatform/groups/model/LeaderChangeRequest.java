package edu.aiplatform.groups.model;

import java.time.Instant;
import java.util.UUID;

/** Yêu cầu đổi trưởng nhóm (BR-U12-10, 11); tối đa một {@code PENDING} mỗi nhóm. */
public record LeaderChangeRequest(UUID id, UUID groupId, UUID requestedBy, UUID proposedLeaderId, String reason,
                                  Status status, UUID newLeaderId, UUID decidedBy, Instant decidedAt,
                                  String decisionReason, Instant createdAt) {

    public enum Status {
        PENDING, APPROVED, REJECTED, CANCELLED
    }

    public LeaderChangeRequest decide(Status next, UUID newLeader, UUID by, String why, Instant now) {
        return new LeaderChangeRequest(id, groupId, requestedBy, proposedLeaderId, reason, next, newLeader, by, now, why,
                createdAt);
    }
}
