package edu.aiplatform.assignmentwork.application;

import static edu.aiplatform.assignmentwork.application.WorkFixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.aiplatform.assignments.contract.AssignmentStatus;
import edu.aiplatform.assignments.contract.GradingMode;
import edu.aiplatform.assignments.contract.SubmissionMode;
import edu.aiplatform.assignmentwork.contract.ActivityEventInput;
import edu.aiplatform.assignmentwork.contract.ActivityKind;
import edu.aiplatform.assignmentwork.contract.ActivityPolicyState;
import edu.aiplatform.assignmentwork.contract.ActivitySummary;
import edu.aiplatform.assignmentwork.contract.AttemptStatus;
import edu.aiplatform.assignmentwork.contract.IntervalCertainty;
import edu.aiplatform.assignmentwork.contract.WorkKind;
import edu.aiplatform.assignmentwork.contract.WorkRef;
import edu.aiplatform.assignmentwork.domain.ActivityEvent;
import edu.aiplatform.assignmentwork.domain.ActivityEventRepository;
import edu.aiplatform.assignmentwork.domain.AssignmentAttempt;
import edu.aiplatform.shared.error.ApiException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/** F11, F12; BR-U08-50…54; P4. */
class ActivityTest {

    private final WorkFixtures f = new WorkFixtures();
    private final ActivityEventRepository events = mock(ActivityEventRepository.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final ActivityService service = new ActivityService(events, f.attempts, f.access, redis, f.clock, 60);
    private final UUID session = UUID.randomUUID();
    private long sequence;

    private ActivityEvent event(ActivityKind kind, Instant at) {
        return new ActivityEvent(UUID.randomUUID(), f.student.accountId(), f.classId, WorkKind.ASSIGNMENT_ATTEMPT,
                UUID.randomUUID(), session, UUID.randomUUID().toString(), sequence++, kind, at, at, 1);
    }

    private static String code(Runnable action) {
        try {
            action.run();
        } catch (ApiException e) {
            return e.code();
        }
        throw new AssertionError("Cần lỗi nghiệp vụ");
    }

    private AssignmentAttempt monitored(AttemptStatus status) {
        f.view = new edu.aiplatform.assignments.contract.AssignmentView(f.assignmentId,
                edu.aiplatform.assignments.contract.AssignmentScope.CLASS, f.classId, f.subjectId, 1, "Bài", SubmissionMode.INDIVIDUAL,
                GradingMode.GRADED, false, AssignmentStatus.OPEN, NOW.minus(Duration.ofDays(1)), NOW.plus(Duration.ofDays(1)),
                null, 2, true, "Có ghi nhận", List.of());
        return f.attempt(1, status, NOW.plus(Duration.ofDays(1)), false);
    }

    private List<ActivityEventInput> batch(int size) {
        List<ActivityEventInput> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            list.add(new ActivityEventInput(session, "e" + i, i, ActivityKind.HEARTBEAT, NOW));
        }
        return list;
    }

    @Test
    void summaryPairsHiddenVisibleAndMarksGapsUnknown() {
        Instant t = NOW;
        List<ActivityEvent> list = List.of(event(ActivityKind.SESSION_START, t), event(ActivityKind.HIDDEN, t.plusSeconds(10)),
                event(ActivityKind.VISIBLE, t.plusSeconds(70)), event(ActivityKind.HEARTBEAT, t.plusSeconds(400)),
                event(ActivityKind.HIDDEN, t.plusSeconds(410)));
        ActivitySummary summary = ActivitySummaryCalculator.summarize(list);
        assertThat(summary.policyState()).isEqualTo(ActivityPolicyState.AVAILABLE);
        assertThat(summary.awayCount()).isEqualTo(2);
        assertThat(summary.knownAwayDuration()).isEqualTo(Duration.ofSeconds(60));
        assertThat(summary.intervals()).extracting(i -> i.certainty())
                .containsExactly(IntervalCertainty.KNOWN, IntervalCertainty.UNKNOWN, IntervalCertainty.UNKNOWN);
        assertThat(summary.unknownGaps()).isEqualTo(2);
        assertThat(summary.reliabilityNote()).contains("không kết luận gian lận");
        assertThat(ActivitySummaryCalculator.summarize(List.of()).policyState()).isEqualTo(ActivityPolicyState.NO_DATA);
    }

    @Test
    void missingSequenceCountsAsUnknown() {
        ActivityEvent first = event(ActivityKind.SESSION_START, NOW);
        sequence += 3;
        ActivityEvent later = event(ActivityKind.HEARTBEAT, NOW.plusSeconds(60));
        assertThat(ActivitySummaryCalculator.summarize(List.of(first, later)).unknownGaps()).isEqualTo(1);
    }

    @Test
    void recordingNeedsPolicyAndInProgressAttempt() {
        AssignmentAttempt plain = f.attempt(1, AttemptStatus.IN_PROGRESS, NOW.plus(Duration.ofDays(1)), false);
        assertThat(code(() -> service.recordForAttempt(f.student, plain.id(), batch(1)))).isEqualTo("ACTIVITY_DISABLED");
        AssignmentAttempt submitted = monitored(AttemptStatus.SUBMITTED);
        assertThat(code(() -> service.recordForAttempt(f.student, submitted.id(), batch(1)))).isEqualTo("ATTEMPT_SUBMITTED");
        verify(events, never()).insertIgnore(any());
    }

    @Test
    void rateLimitAndRedisFailOpen() {
        AssignmentAttempt a = monitored(AttemptStatus.IN_PROGRESS);
        when(redis.opsForValue()).thenReturn(values);
        when(values.increment(anyString(), anyLong())).thenReturn(61L);
        assertThat(code(() -> service.recordForAttempt(f.student, a.id(), batch(1)))).isEqualTo("ACTIVITY_RATE_LIMITED");
        when(values.increment(anyString(), anyLong())).thenThrow(new RedisConnectionFailureException("down"));
        when(events.insertIgnore(any())).thenReturn(1);
        assertThat(service.recordForAttempt(f.student, a.id(), batch(3))).isEqualTo(3);
        assertThat(code(() -> service.recordForAttempt(f.student, a.id(), batch(51)))).isEqualTo("ACTIVITY_BATCH_TOO_LARGE");
        verify(events, times(3)).insertIgnore(any());
    }

    @Test
    void summaryRespectsPolicyAndRetention() {
        AssignmentAttempt plain = f.attempt(1, AttemptStatus.SUBMITTED, NOW, false);
        assertThat(service.summary(new WorkRef(WorkKind.ASSIGNMENT_ATTEMPT, plain.id(), f.classId), f.student.accountId())
                .policyState()).isEqualTo(ActivityPolicyState.DISABLED);
        AssignmentAttempt monitored = monitored(AttemptStatus.SUBMITTED);
        WorkRef ref = new WorkRef(WorkKind.ASSIGNMENT_ATTEMPT, monitored.id(), f.classId);
        assertThat(service.summary(ref, f.student.accountId()).policyState()).isEqualTo(ActivityPolicyState.NO_DATA);
        ActivityService later = new ActivityService(events, f.attempts, f.access, redis,
                java.time.Clock.fixed(NOW.plus(Duration.ofDays(3)), java.time.ZoneOffset.UTC), 60);
        assertThat(later.summary(ref, f.student.accountId()).policyState()).isEqualTo(ActivityPolicyState.EXPIRED);
    }

    @Test
    void retentionScannerDeletesEventsOlderThanOneDayInBatches() {
        new ActivityRetentionScanner(events, f.transactions).scan(NOW);
        verify(events).deleteOlderThan(NOW.minus(Duration.ofDays(1)), ActivityRetentionScanner.BATCH);
    }
}
