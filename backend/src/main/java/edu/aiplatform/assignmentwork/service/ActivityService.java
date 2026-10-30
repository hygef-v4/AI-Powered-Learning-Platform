package edu.aiplatform.assignmentwork.service;

import edu.aiplatform.assignmentwork.contract.ActivityEventInput;
import edu.aiplatform.assignmentwork.contract.ActivityPolicyState;
import edu.aiplatform.assignmentwork.contract.ActivitySummary;
import edu.aiplatform.assignmentwork.contract.IActivityQuery;
import edu.aiplatform.assignmentwork.contract.IActivityWrite;
import edu.aiplatform.assignmentwork.contract.WorkKind;
import edu.aiplatform.assignmentwork.contract.WorkRef;
import edu.aiplatform.assignmentwork.model.ActivityEvent;
import edu.aiplatform.assignmentwork.repository.ActivityEventRepository;
import edu.aiplatform.assignmentwork.model.AssignmentAttempt;
import edu.aiplatform.assignmentwork.repository.AttemptRepository;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cài {@link IActivityWrite}, {@link IActivityQuery} (F11, F12; BR-U08-50…55): chỉ thu khi chính sách của lượt bật và
 * lượt đang làm; bỏ trùng theo {@code clientEventId}; tối đa 60 sự kiện mỗi phút mỗi công việc (bộ đếm Redis, Redis
 * lỗi thì vẫn ghi); giờ server là giờ ghi; tóm tắt tính khi đọc.
 */
@Service
public class ActivityService implements IActivityWrite, IActivityQuery {

    /** Thời hạn giữ sự kiện hoạt động (BR-U08-54), cố định 1 ngày; quá hạn thì tự xóa. */
    public static final Duration RETENTION = Duration.ofDays(1);
    static final int MAX_BATCH = 50;

    private final ActivityEventRepository events;
    private final AttemptRepository attempts;
    private final WorkAccess access;
    private final StringRedisTemplate redis;
    private final Clock clock;
    private final int perMinute;

    public ActivityService(ActivityEventRepository events, AttemptRepository attempts, WorkAccess access,
                           StringRedisTemplate redis, Clock clock,
                           @Value("${platform.assignment-work.activity-events-per-minute:60}") int perMinute) {
        this.events = events;
        this.attempts = attempts;
        this.access = access;
        this.redis = redis;
        this.clock = clock;
        this.perMinute = perMinute;
    }

    /** F11 cho lượt làm bài: chủ lượt, lượt đang làm, chính sách trong ảnh chụp bật (BR-U08-50, 52). */
    @Transactional
    public int recordForAttempt(Actor actor, UUID attemptId, List<ActivityEventInput> input) {
        AssignmentAttempt attempt = access.requireOwnAttempt(actor, attemptId, "ACTIVITY_RECORDED");
        if (!attempt.snapshot().activityMonitoringEnabled()) {
            throw ApiException.conflict("ACTIVITY_DISABLED", "Bài không ghi nhận hoạt động");
        }
        if (!attempt.inProgress()) {
            throw ApiException.conflict("ATTEMPT_SUBMITTED", "Lượt đã nộp");
        }
        return insert(actor, new WorkRef(WorkKind.ASSIGNMENT_ATTEMPT, attempt.id(), attempt.classId()), input);
    }

    /** U11, U12 đã kiểm quyền và chính sách của công việc rồi gọi. */
    @Override
    @Transactional
    public void record(Actor actor, WorkRef workRef, List<ActivityEventInput> input) {
        insert(actor, workRef, input);
    }

    @Override
    @Transactional(readOnly = true)
    public ActivitySummary summary(WorkRef workRef, UUID accountId) {
        Optional<AssignmentAttempt> attempt = workRef.kind() == WorkKind.ASSIGNMENT_ATTEMPT
                ? attempts.find(workRef.workId()) : Optional.empty();
        if (attempt.isPresent() && !attempt.get().snapshot().activityMonitoringEnabled()) {
            return new ActivitySummary(ActivityPolicyState.DISABLED, 0, Duration.ZERO, List.of(), 0,
                    "Bài không bật ghi nhận hoạt động.");
        }
        List<ActivityEvent> list = events.list(workRef.kind(), workRef.workId(), accountId);
        if (list.isEmpty() && attempt.isPresent() && expired(attempt.get().startedAt())) {
            return new ActivitySummary(ActivityPolicyState.EXPIRED, 0, Duration.ZERO, List.of(), 0,
                    "Dữ liệu đã quá thời hạn lưu và bị xóa.");
        }
        return ActivitySummaryCalculator.summarize(list);
    }

    private int insert(Actor actor, WorkRef workRef, List<ActivityEventInput> input) {
        if (actor == null || actor.accountId() == null || workRef == null || workRef.workId() == null
                || workRef.classId() == null) {
            throw ApiException.badRequest("ACTIVITY_INVALID", "Thiếu thông tin công việc");
        }
        if (input == null || input.isEmpty()) {
            return 0;
        }
        if (input.size() > MAX_BATCH) {
            throw ApiException.badRequest("ACTIVITY_BATCH_TOO_LARGE", "Mỗi lô tối đa 50 sự kiện");
        }
        for (ActivityEventInput event : input) {
            if (event == null || event.sessionId() == null || event.kind() == null || event.sequence() < 0
                    || event.clientEventId() == null || event.clientEventId().isBlank()
                    || event.clientEventId().length() > 64) {
                throw ApiException.badRequest("ACTIVITY_INVALID", "Sự kiện không hợp lệ");
            }
        }
        if (!allow(actor.accountId(), workRef.workId(), input.size())) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "ACTIVITY_RATE_LIMITED", "Gửi sự kiện quá nhanh");
        }
        Instant now = clock.instant();
        int written = 0;
        for (ActivityEventInput event : input) {
            written += events.insertIgnore(new ActivityEvent(UUID.randomUUID(), actor.accountId(), workRef.classId(),
                    workRef.kind(), workRef.workId(), event.sessionId(), event.clientEventId(), event.sequence(),
                    event.kind(), event.clientTime(), now, 1));
        }
        return written;
    }

    /** Bộ đếm theo phút trên Redis; Redis lỗi thì cho qua để không mất dữ liệu bài làm (BR-U08-62). */
    boolean allow(UUID accountId, UUID workId, int count) {
        String key = "activity-rate:" + accountId + ":" + workId + ":" + clock.instant().getEpochSecond() / 60;
        try {
            Long total = redis.opsForValue().increment(key, count);
            if (total != null && total == count) {
                redis.expire(key, Duration.ofMinutes(2));
            }
            return total == null || total <= perMinute;
        } catch (RuntimeException e) {
            return true;
        }
    }

    private boolean expired(Instant startedAt) {
        return startedAt.isBefore(clock.instant().minus(RETENTION));
    }
}
