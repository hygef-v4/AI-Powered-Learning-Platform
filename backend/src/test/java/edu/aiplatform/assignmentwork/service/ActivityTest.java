package edu.aiplatform.assignmentwork.service;

import static edu.aiplatform.assignmentwork.service.WorkFixtures.NOW;
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
import edu.aiplatform.assignmentwork.model.ActivityEvent;
import edu.aiplatform.assignmentwork.repository.ActivityEventRepository;
import edu.aiplatform.assignmentwork.model.AssignmentAttempt;
import edu.aiplatform.shared.error.ApiException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
            return e.readCode();
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
    void missingSequenceCountsAsUnknown() {
        ActivityEvent first = event(ActivityKind.SESSION_START, NOW);
        sequence += 3;
        ActivityEvent later = event(ActivityKind.HEARTBEAT, NOW.plusSeconds(60));
        assertThat(ActivitySummaryCalculator.summarize(List.of(first, later)).unknownGaps()).isEqualTo(1);
    }
}
