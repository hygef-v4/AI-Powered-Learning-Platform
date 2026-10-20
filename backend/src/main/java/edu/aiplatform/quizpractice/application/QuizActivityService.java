package edu.aiplatform.quizpractice.application;

import edu.aiplatform.assignmentwork.contract.ActivityEventInput;
import edu.aiplatform.assignmentwork.contract.IActivityWrite;
import edu.aiplatform.assignmentwork.contract.WorkKind;
import edu.aiplatform.assignmentwork.contract.WorkRef;
import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.quizpractice.domain.QuizAttempt;
import edu.aiplatform.shared.error.ApiException;
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

    public QuizActivityService(IActivityWrite activity, PracticeSupport support) {
        this.activity = activity;
        this.support = support;
    }

    @Transactional
    public void record(Actor actor, UUID attemptId, List<ActivityEventInput> events) {
        QuizAttempt attempt = support.ownAttempt(actor, attemptId, Actions.QUIZ_PRACTICE);
        if (!attempt.inProgress()) {
            throw ApiException.conflict("ATTEMPT_SUBMITTED", "Lượt này đã nộp");
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
