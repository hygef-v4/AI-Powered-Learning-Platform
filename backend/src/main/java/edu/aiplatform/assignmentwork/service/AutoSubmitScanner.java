package edu.aiplatform.assignmentwork.service;

import edu.aiplatform.assignmentwork.contract.SubmitMode;
import edu.aiplatform.assignmentwork.model.AssignmentAttempt;
import edu.aiplatform.assignmentwork.repository.AttemptRepository;
import edu.aiplatform.jobs.contract.IScheduledScanner;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** F7 (BR-U08-23): mỗi phút nộp lượt đang làm đã qua hạn cộng 30 giây, mỗi lượt một transaction. */
@Component
public class AutoSubmitScanner implements IScheduledScanner {

    private static final Logger log = LoggerFactory.getLogger(AutoSubmitScanner.class);
    static final int BATCH = 200;

    private final AttemptRepository attempts;
    private final AttemptSubmitter submitter;
    private final TransactionTemplate transaction;

    public AutoSubmitScanner(AttemptRepository attempts, AttemptSubmitter submitter,
                             PlatformTransactionManager transactions) {
        this.attempts = attempts;
        this.submitter = submitter;
        this.transaction = new TransactionTemplate(transactions);
    }

    @Override
    public String name() {
        return "assignment-auto-submit";
    }

    @Override
    public void scan(Instant now) {
        for (UUID id : attempts.overdue(now.minus(AssignmentAttempt.GRACE), BATCH)) {
            try {
                transaction.executeWithoutResult(status -> submitter.autoSubmit(id, SubmitMode.AUTO_DEADLINE, now));
            } catch (RuntimeException e) {
                log.warn("Tự nộp lượt {} lỗi", id, e);
            }
        }
    }
}
