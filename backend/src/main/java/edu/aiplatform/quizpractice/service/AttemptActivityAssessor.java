package edu.aiplatform.quizpractice.service;

import edu.aiplatform.assignmentwork.contract.ActivityPolicyState;
import edu.aiplatform.assignmentwork.contract.ActivitySummary;
import edu.aiplatform.assignmentwork.contract.IActivityQuery;
import edu.aiplatform.assignmentwork.contract.IntervalCertainty;
import edu.aiplatform.assignmentwork.contract.WorkKind;
import edu.aiplatform.assignmentwork.contract.WorkRef;
import edu.aiplatform.quizpractice.model.ActivityOutcome;
import edu.aiplatform.quizpractice.model.ActivitySignal;
import edu.aiplatform.quizpractice.model.QuizAttempt;
import edu.aiplatform.quizzes.contract.LeavePenalty;
import java.math.BigDecimal;
import java.time.Duration;
import org.springframework.stereotype.Component;

/**
 * Đánh giá rời trang lúc nộp (BR-U11-32, 33; FR-039 sửa 2026-11-02). Hệ thống không tự trừ điểm và không kết luận gian
 * lận: chỉ tính mức trừ gợi ý để giảng viên quyết định, điểm của lượt giữ nguyên. Tín hiệu do trình duyệt tự báo nên có
 * thể bị chặn: không có tín hiệu thì chỉ gắn cờ. Chỉ đếm khoảng rời chắc chắn và đủ dài. Mức gợi ý không vượt điểm gốc.
 */
@Component
class AttemptActivityAssessor {

    /** Đồng bộ với khoảng heartbeat tối đa của U08 (BR-U08-53). */
    static final Duration SIGNAL_GAP = Duration.ofMinutes(2);

    private final IActivityQuery activity;

    AttemptActivityAssessor(IActivityQuery activity) {
        this.activity = activity;
    }

    /** Lượt đã chấm; Quiz không bật theo dõi thì giữ nguyên. */
    QuizAttempt assess(QuizAttempt scored) {
        if (!scored.snapshot().activityMonitoringEnabled()) {
            return scored;
        }
        BigDecimal raw = scored.score();
        ActivitySummary summary;
        try {
            summary = activity.summary(new WorkRef(WorkKind.QUIZ_ATTEMPT, scored.id(), scored.classId()), scored.accountId());
        } catch (RuntimeException unavailable) {
            return scored.withOutcome(unavailable(raw), raw);
        }
        if (summary == null) {
            return scored.withOutcome(unavailable(raw), raw);
        }
        LeavePenalty policy = scored.snapshot().settings().leavePenalty();
        Duration minAway = Duration.ofSeconds(policy == null ? 0 : policy.minAwaySeconds());
        int leaves = (int) summary.intervals().stream()
                .filter(i -> i.certainty() == IntervalCertainty.KNOWN)
                .filter(i -> i.end() != null && Duration.between(i.start(), i.end()).compareTo(minAway) >= 0)
                .count();
        return scored.withOutcome(new ActivityOutcome(raw, suggestedPenalty(policy, leaves, raw), BigDecimal.ZERO, leaves,
                signalOf(summary, scored), null, null, null, null), raw);
    }

    private static ActivityOutcome unavailable(BigDecimal raw) {
        return new ActivityOutcome(raw, BigDecimal.ZERO, BigDecimal.ZERO, null, ActivitySignal.UNAVAILABLE, null, null, null,
                null);
    }

    static BigDecimal suggestedPenalty(LeavePenalty policy, int leaves, BigDecimal raw) {
        if (policy == null || leaves <= policy.freeLeaves()) {
            return BigDecimal.ZERO;
        }
        BigDecimal penalty = policy.pointsPerLeave().multiply(BigDecimal.valueOf(leaves - policy.freeLeaves()));
        if (policy.maxPenalty() != null) {
            penalty = penalty.min(policy.maxPenalty());
        }
        return penalty.min(raw);
    }

    private static ActivitySignal signalOf(ActivitySummary summary, QuizAttempt attempt) {
        if (summary.policyState() != ActivityPolicyState.AVAILABLE) {
            return ActivitySignal.NO_SIGNAL;
        }
        boolean savedAfterSilence = summary.lastEventAt() != null && attempt.lastSavedAt() != null
                && attempt.lastSavedAt().isAfter(summary.lastEventAt().plus(SIGNAL_GAP));
        return summary.unknownGaps() > 0 || savedAfterSilence ? ActivitySignal.SIGNAL_LOST : ActivitySignal.OK;
    }
}
