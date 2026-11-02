package edu.aiplatform.assignments.service;

import edu.aiplatform.assignments.repository.AssignmentRepository;
import edu.aiplatform.jobs.contract.IScheduledScanner;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * F10 (P3): mỗi phút trong worker mở bài tới {@code opensAt} (mỗi bài một transaction: cập nhật có điều kiện,
 * listener, sự kiện sau commit) rồi đóng bài qua {@code lateUntil} hoặc {@code closesAt}.
 */
@Component
public class AssignmentScheduleScanner implements IScheduledScanner {

    private static final Logger log = LoggerFactory.getLogger(AssignmentScheduleScanner.class);
    static final int BATCH = 200;

    private final AssignmentRepository assignments;
    private final AssignmentLifecycle lifecycle;
    private final TransactionTemplate transaction;

    public AssignmentScheduleScanner(AssignmentRepository assignments, AssignmentLifecycle lifecycle,
                                     PlatformTransactionManager transactions) {
        this.assignments = assignments;
        this.lifecycle = lifecycle;
        this.transaction = new TransactionTemplate(transactions);
    }

    @Override
    public String name() {
        return "assignment-schedule";
    }

    @Override
    public void scan(Instant now) {
        for (UUID id : assignments.dueToOpen(now, BATCH)) {
            try {
                transaction.executeWithoutResult(status -> lifecycle.open(id, now));
            } catch (RuntimeException e) {
                log.warn("Mở bài {} theo lịch lỗi", id, e);
            }
        }
        List<UUID> closed = transaction.execute(status -> assignments.closeDue(now));
        if (closed != null && !closed.isEmpty()) {
            log.info("Đóng {} bài theo lịch", closed.size());
        }
    }
}
