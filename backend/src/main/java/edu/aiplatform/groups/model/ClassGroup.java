package edu.aiplatform.groups.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Nhóm cố định của lớp (domain-entities U12 mục 2); mọi bài nhóm của lớp dùng chung (BR-U12-01). */
public record ClassGroup(UUID id, UUID classId, String name, UUID leaderId, UUID createdBy, Instant createdAt,
                         Instant updatedAt, List<UUID> memberIds) {

    public ClassGroup {
        memberIds = memberIds == null ? List.of() : List.copyOf(memberIds);
    }

    public boolean isMember(UUID accountId) {
        return memberIds.contains(accountId);
    }
}
