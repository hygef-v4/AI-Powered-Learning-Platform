package edu.aiplatform.assignmentwork.service;

import edu.aiplatform.assignments.contract.IAssignmentLifecycleListener;
import edu.aiplatform.assignmentwork.contract.SubmitMode;
import edu.aiplatform.assignmentwork.repository.AttemptRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Cài {@link IAssignmentLifecycleListener} (F7 bước 2): U07 ngưng giao thì trong cùng transaction nộp mọi lượt đang
 * làm của bài với {@code AUTO_RETIRED}; mở bài không cần làm gì.
 */
@Component
public class AssignmentRetiredListener implements IAssignmentLifecycleListener {

    private final AttemptRepository attempts;
    private final AttemptSubmitter submitter;
    private final Clock clock;

    public AssignmentRetiredListener(AttemptRepository attempts, AttemptSubmitter submitter, Clock clock) {
        this.attempts = attempts;
        this.submitter = submitter;
        this.clock = clock;
    }

    @Override
    public void onOpened(UUID assignmentId) {
    }

    @Override
    public void onRetired(UUID assignmentId) {
        Instant now = clock.instant();
        for (UUID attemptId : attempts.inProgressOf(assignmentId)) {
            submitter.autoSubmit(attemptId, SubmitMode.AUTO_RETIRED, now);
        }
    }
}
