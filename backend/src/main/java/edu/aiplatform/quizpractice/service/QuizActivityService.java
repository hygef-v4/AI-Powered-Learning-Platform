package edu.aiplatform.quizpractice.service;

import edu.aiplatform.assignmentwork.contract.ActivityEventInput;
import edu.aiplatform.assignmentwork.contract.IActivityWrite;
import edu.aiplatform.assignmentwork.contract.WorkKind;
import edu.aiplatform.assignmentwork.contract.WorkRef;
import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.quizpractice.model.QuizAttempt;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ghi dữ liệu rời trang của lượt Quiz (F8, BR-U11-30): chủ lượt, lượt đang làm, chính sách đã chụp bật; U08 bỏ trùng,
 * giới hạn tần suất, ghi giờ server. Trình duyệt gửi sự kiện khi U08 có {@code useActivityTracker} (G3).
 */
@Service
public class QuizActivityService {

    public static final int MAX_EVENTS = 100;

    private final IActivityWrite activity;
    private final PracticeSupport support;
    private final Clock clock;

    public QuizActivityService(IActivityWrite activity, PracticeSupport support, Clock clock) {
        this.activity = activity;
        this.support = support;
        this.clock = clock;
    }

    @Transactional
    public void record(Actor actor, UUID attemptId, List<ActivityEventInput> events) {
        QuizAttempt attempt = support.ownAttempt(actor, attemptId, Actions.QUIZ_PRACTICE);
        if (!attempt.inProgress()) {
            throw ApiException.conflict("ATTEMPT_SUBMITTED", "Lượt này đã nộp");
        }
        // Quá hạn cộng ân hạn là hết giờ làm bài, dù scanner chưa kịp tự nộp: không nhận thêm hoạt động như lưu câu trả lời.
        if (!attempt.acceptsAnswersAt(clock.instant())) {
            throw ApiException.conflict("ATTEMPT_TIME_UP", "Đã hết thời gian làm bài");
        }
        if (!attempt.snapshot().activityMonitoringEnabled()) {
            throw ApiException.conflict("ACTIVITY_NOT_ENABLED", "Quiz này không ghi nhận hoạt động");
        }
        if (events.isEmpty() || events.size() > MAX_EVENTS) {
            throw ApiException.badRequest("ACTIVITY_BATCH_INVALID", "Mỗi lần gửi 1–100 sự kiện");
        }
        try {
            activity.record(actor, new WorkRef(WorkKind.QUIZ_ATTEMPT, attemptId, attempt.classId()), events);
        } catch (ApiException rejected) {
            throw rejected;
        } catch (RuntimeException unavailable) {
            throw ApiException.unavailable("ACTIVITY_UNAVAILABLE", "Chưa ghi được hoạt động, vui lòng thử lại");
        }
    }
}
