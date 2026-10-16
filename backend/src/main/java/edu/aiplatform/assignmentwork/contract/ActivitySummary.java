package edu.aiplatform.assignmentwork.contract;

import java.time.Duration;
import java.util.List;

/** Tính khi đọc, không lưu; chỉ là dấu hiệu rời trang, không kết luận gian lận. */
public record ActivitySummary(ActivityPolicyState policyState, int awayCount, Duration knownAwayDuration,
                              List<ActivityInterval> intervals, int unknownGaps, String reliabilityNote) {
}
