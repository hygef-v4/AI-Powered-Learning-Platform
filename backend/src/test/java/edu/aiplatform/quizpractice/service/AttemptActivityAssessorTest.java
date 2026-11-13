package edu.aiplatform.quizpractice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import edu.aiplatform.assignmentwork.contract.ActivityInterval;
import edu.aiplatform.assignmentwork.contract.ActivityPolicyState;
import edu.aiplatform.assignmentwork.contract.ActivitySummary;
import edu.aiplatform.assignmentwork.contract.IActivityQuery;
import edu.aiplatform.assignmentwork.contract.IntervalCertainty;
import edu.aiplatform.quizpractice.model.ActivitySignal;
import edu.aiplatform.quizpractice.model.AttemptSnapshot;
import edu.aiplatform.quizpractice.model.QuestionResult;
import edu.aiplatform.quizpractice.model.QuizAttempt;
import edu.aiplatform.quizpractice.model.SubmitMode;
import edu.aiplatform.quizzes.contract.LeavePenalty;
import edu.aiplatform.quizzes.contract.QuizSettings;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Tính mức trừ gợi ý và cờ tín hiệu lúc nộp; điểm không tự đổi (BR-U11-32, 33). */
class AttemptActivityAssessorTest {

    private static final Instant NOW = Instant.parse("2026-11-01T01:00:00Z");

    private final IActivityQuery activity = mock(IActivityQuery.class);
    private final AttemptActivityAssessor assessor = new AttemptActivityAssessor(activity);

    private static LeavePenalty policy() {
        return new LeavePenalty(1, new BigDecimal("0.5"), null, 5);
    }

    private static QuizAttempt scored(boolean monitoring, LeavePenalty penalty, String raw, Instant lastSaved) {
        QuizSettings settings = new QuizSettings(false, false, null, null, true, true, penalty);
        AttemptSnapshot snapshot = new AttemptSnapshot(1, "Quiz", settings, monitoring, "Lời báo", 1, List.of(), Map.of(),
                new BigDecimal("10"));
        QuizAttempt attempt = QuizAttempt.start(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1, snapshot,
                NOW.minus(Duration.ofHours(1)));
        if (lastSaved != null) {
            attempt = attempt.withAnswers(Map.of(), lastSaved);
        }
        return attempt.submitted(SubmitMode.MANUAL, new BigDecimal(raw), new BigDecimal("10"), List.<QuestionResult>of(), NOW);
    }

    private static ActivityInterval away(int seconds, IntervalCertainty certainty) {
        Instant start = NOW.minus(Duration.ofMinutes(10));
        return new ActivityInterval(start, start.plusSeconds(seconds), certainty);
    }

    private void summary(int unknownGaps, Instant lastEvent, ActivityInterval... intervals) {
        when(activity.summary(any(), any())).thenReturn(new ActivitySummary(ActivityPolicyState.AVAILABLE, intervals.length,
                Duration.ZERO, List.of(intervals), unknownGaps, "", lastEvent));
    }

    @Test
    void exampleFromTheReportFourLeavesOneFreeHalfPointEach() {
        summary(0, NOW, away(10, IntervalCertainty.KNOWN), away(10, IntervalCertainty.KNOWN),
                away(10, IntervalCertainty.KNOWN), away(10, IntervalCertainty.KNOWN));
        QuizAttempt result = assessor.assess(scored(true, policy(), "8", null));
        assertThat(result.outcome().rawScore()).isEqualByComparingTo("8");
        assertThat(result.outcome().suggestedPenalty()).isEqualByComparingTo("1.5");
        assertThat(result.outcome().penaltyPoints()).isEqualByComparingTo("0");
        assertThat(result.outcome().pendingDecision()).isTrue();
        assertThat(result.outcome().leaveCount()).isEqualTo(4);
        assertThat(result.outcome().signal()).isEqualTo(ActivitySignal.OK);
        assertThat(result.score()).as("không tự trừ khi chưa có quyết định").isEqualByComparingTo("8");
    }

    @Test
    void shortOrUncertainLeavesAreNotCounted() {
        summary(0, NOW, away(3, IntervalCertainty.KNOWN), away(60, IntervalCertainty.UNKNOWN), away(5, IntervalCertainty.KNOWN));
        QuizAttempt result = assessor.assess(scored(true, policy(), "8", null));
        assertThat(result.outcome().leaveCount()).isEqualTo(1);
        assertThat(result.outcome().suggestedPenalty()).isEqualByComparingTo("0");
        assertThat(result.outcome().pendingDecision()).isFalse();
        assertThat(result.score()).isEqualByComparingTo("8");
    }

    @Test
    void suggestionIsCappedByMaximumAndNeverBelowZero() {
        assertThat(AttemptActivityAssessor.suggestedPenalty(new LeavePenalty(0, new BigDecimal("2"), new BigDecimal("3"), 0), 5, new BigDecimal("8")))
                .isEqualByComparingTo("3");
        assertThat(AttemptActivityAssessor.suggestedPenalty(new LeavePenalty(0, new BigDecimal("2"), null, 0), 5, new BigDecimal("1")))
                .isEqualByComparingTo("1");
        assertThat(AttemptActivityAssessor.suggestedPenalty(null, 5, new BigDecimal("8"))).isEqualByComparingTo("0");
    }

    @Test
    void noMonitoringKeepsScoreAndRecordsNothing() {
        QuizAttempt result = assessor.assess(scored(false, null, "8", null));
        assertThat(result.outcome()).isNull();
        assertThat(result.score()).isEqualByComparingTo("8");
    }

    @Test
    void blockedTrackerIsFlaggedButNeverPenalizedOrTreatedAsClean() {
        when(activity.summary(any(), any())).thenReturn(new ActivitySummary(ActivityPolicyState.NO_DATA, 0, Duration.ZERO,
                List.of(), 0, ""));
        QuizAttempt result = assessor.assess(scored(true, policy(), "8", null));
        assertThat(result.outcome().signal()).isEqualTo(ActivitySignal.NO_SIGNAL);
        assertThat(result.outcome().suggestedPenalty()).isEqualByComparingTo("0");
        assertThat(result.score()).isEqualByComparingTo("8");
    }

    @Test
    void savingAnswersAfterEventsStoppedOrHeartbeatGapsFlagSignalLost() {
        summary(0, NOW.minus(Duration.ofMinutes(30)));
        assertThat(assessor.assess(scored(true, policy(), "8", NOW.minus(Duration.ofMinutes(5)))).outcome().signal())
                .isEqualTo(ActivitySignal.SIGNAL_LOST);
        assertThat(assessor.assess(scored(true, policy(), "8", null)).outcome().signal())
                .as("nộp bài quá 2 phút sau sự kiện cuối cũng gắn cờ mất tín hiệu")
                .isEqualTo(ActivitySignal.SIGNAL_LOST);
        summary(1, NOW);
        assertThat(assessor.assess(scored(true, policy(), "8", null)).outcome().signal()).isEqualTo(ActivitySignal.SIGNAL_LOST);
        summary(0, NOW.minus(Duration.ofMinutes(1)));
        assertThat(assessor.assess(scored(true, policy(), "8", NOW)).outcome().signal()).isEqualTo(ActivitySignal.OK);
    }

    @Test
    void unavailableActivityNeverBlocksSubmission() {
        when(activity.summary(any(), any())).thenThrow(new IllegalStateException("down"));
        QuizAttempt result = assessor.assess(scored(true, policy(), "8", null));
        assertThat(result.outcome().signal()).isEqualTo(ActivitySignal.UNAVAILABLE);
        assertThat(result.score()).isEqualByComparingTo("8");
        assertThat(result.outcome().leaveCount()).isNull();
    }
    @Test
    void lateFirstEventFlagsInitialGapWithoutChangingScore() {
        when(activity.summary(any(), any())).thenReturn(new ActivitySummary(ActivityPolicyState.AVAILABLE, 0,
                Duration.ZERO, List.of(), 0, "", NOW, NOW.minusSeconds(60)));
        var result = assessor.assess(scored(true, policy(), "8", null));
        assertThat(result.outcome().signal()).isEqualTo(ActivitySignal.SIGNAL_LOST);
        assertThat(result.score()).isEqualByComparingTo("8");
    }
}
