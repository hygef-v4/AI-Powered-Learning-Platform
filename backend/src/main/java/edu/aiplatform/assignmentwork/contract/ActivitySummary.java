package edu.aiplatform.assignmentwork.contract;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Tính khi đọc, không lưu; chỉ là dấu hiệu rời trang do trình duyệt tự báo, có thể bị chặn hoặc giả, không phải bằng
 * chứng gian lận. {@code lastEventAt}: giờ server của sự kiện cuối.
 */
public record ActivitySummary(ActivityPolicyState policyState, int awayCount, Duration knownAwayDuration,
                              List<ActivityInterval> intervals, int unknownGaps, String reliabilityNote,
                              Instant lastEventAt, Instant firstEventAt) {

    public ActivitySummary(ActivityPolicyState policyState, int awayCount, Duration knownAwayDuration,
            List<ActivityInterval> intervals, int unknownGaps, String reliabilityNote, Instant lastEventAt) {
        this(policyState, awayCount, knownAwayDuration, intervals, unknownGaps, reliabilityNote, lastEventAt, null);
    }

    public ActivitySummary(ActivityPolicyState policyState, int awayCount, Duration knownAwayDuration,
                           List<ActivityInterval> intervals, int unknownGaps, String reliabilityNote) {
        this(policyState, awayCount, knownAwayDuration, intervals, unknownGaps, reliabilityNote, null);
    }
}
