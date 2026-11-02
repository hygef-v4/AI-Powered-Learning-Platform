package edu.aiplatform.groupdocs.model;

import java.time.Instant;
import java.util.UUID;

/** Trưởng nhóm đã chốt phân công tài liệu nhóm (FB-U12-01): từ lúc này mỗi người soạn mục mình phụ trách. */
public record AssignmentLock(UUID lockedBy, Instant lockedAt) {
}
