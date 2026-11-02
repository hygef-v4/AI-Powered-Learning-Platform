package edu.aiplatform.assignmentwork.domain;

import edu.aiplatform.assignmentwork.contract.ActivityKind;
import edu.aiplatform.assignmentwork.contract.WorkKind;
import java.time.Instant;
import java.util.UUID;

/** Sự kiện rời trang chỉ thêm (P4); bỏ trùng theo {@code clientEventId}; giờ server là giờ ghi. */
public record ActivityEvent(UUID id, UUID accountId, UUID classId, WorkKind workKind, UUID workId, UUID sessionId,
                            String clientEventId, long sequence, ActivityKind kind, Instant clientTime,
                            Instant serverTime, int policyVersion) {
}
