package edu.aiplatform.groups.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Lời mời hoặc thành viên của một đề xuất (BR-U12-77, 78); một dòng mỗi cặp đề xuất–sinh viên. {@code active} chỉ
 * đúng khi {@code ACCEPTED} và đề xuất đang hoạt động.
 */
public record ProposalMember(UUID proposalId, UUID classId, UUID accountId, Status status, boolean active,
                             UUID invitedBy, Instant createdAt, Instant respondedAt) {

    public enum Status {
        INVITED, ACCEPTED, DECLINED, REVOKED, EXPIRED, LEFT
    }

    public boolean isInvited() {
        return status == Status.INVITED;
    }

    public boolean isActiveMember() {
        return active && status == Status.ACCEPTED;
    }
}
