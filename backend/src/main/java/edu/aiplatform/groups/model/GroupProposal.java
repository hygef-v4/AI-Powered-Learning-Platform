package edu.aiplatform.groups.model;

import java.time.Instant;
import java.util.UUID;

/** Đề xuất nhóm của sinh viên, chỉ thành nhóm chính thức khi giảng viên duyệt (BR-U12-75…86). */
public record GroupProposal(UUID id, UUID classId, String name, UUID leaderId, Status status, Instant submittedAt,
                            UUID decidedBy, Instant decidedAt, String decisionReason, UUID approvedGroupId,
                            EndReason endReason, Instant createdAt, Instant updatedAt, long version) {

    public enum Status {
        DRAFT, SUBMITTED, APPROVED, REJECTED, CANCELLED;

        /** Đề xuất đang hoạt động: nháp hoặc chờ duyệt. */
        public boolean isActive() {
            return this == DRAFT || this == SUBMITTED;
        }
    }

    /** Vì sao đề xuất bị hủy; chỉ có khi {@code CANCELLED}. */
    public enum EndReason {
        LEADER_CANCELLED, LEADER_PLACED, LEADER_LEFT_CLASS
    }

    public static GroupProposal draft(UUID id, UUID classId, String name, UUID leaderId, Instant now) {
        return new GroupProposal(id, classId, name, leaderId, Status.DRAFT, null, null, null, null, null, null, now, now,
                0);
    }

    public boolean isLeader(UUID accountId) {
        return leaderId.equals(accountId);
    }

    public GroupProposal renamed(String newName, Instant now) {
        return new GroupProposal(id, classId, newName, leaderId, status, submittedAt, decidedBy, decidedAt,
                decisionReason, approvedGroupId, endReason, createdAt, now, version + 1);
    }

    /** Chỉ tăng {@code version}: thành viên hay lời mời của đề xuất vừa đổi. */
    public GroupProposal touched(Instant now) {
        return new GroupProposal(id, classId, name, leaderId, status, submittedAt, decidedBy, decidedAt,
                decisionReason, approvedGroupId, endReason, createdAt, now, version + 1);
    }

    public GroupProposal submitted(Instant now) {
        return new GroupProposal(id, classId, name, leaderId, Status.SUBMITTED, now, null, null, null, null, null,
                createdAt, now, version + 1);
    }

    public GroupProposal backToDraft(Instant now) {
        return new GroupProposal(id, classId, name, leaderId, Status.DRAFT, null, null, null, null, null, null,
                createdAt, now, version + 1);
    }

    public GroupProposal approved(UUID by, UUID groupId, Instant now) {
        return new GroupProposal(id, classId, name, leaderId, Status.APPROVED, submittedAt, by, now, null, groupId,
                null, createdAt, now, version + 1);
    }

    public GroupProposal rejected(UUID by, String reason, Instant now) {
        return new GroupProposal(id, classId, name, leaderId, Status.REJECTED, submittedAt, by, now, reason, null,
                null, createdAt, now, version + 1);
    }

    public GroupProposal cancelled(EndReason reason, Instant now) {
        return new GroupProposal(id, classId, name, leaderId, Status.CANCELLED, submittedAt, null, now, null, null,
                reason, createdAt, now, version + 1);
    }
}
