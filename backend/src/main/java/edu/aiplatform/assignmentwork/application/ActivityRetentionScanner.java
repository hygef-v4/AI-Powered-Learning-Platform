package edu.aiplatform.assignmentwork.application;

import edu.aiplatform.assignmentwork.domain.ActivityEventRepository;
import edu.aiplatform.jobs.contract.IScheduledScanner;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** F12 bước 4 (BR-U08-54; P5): xóa sự kiện cũ hơn {@link ActivityService#RETENTION}, tối đa 10.000 mỗi lần chạy. */
@Component
public class ActivityRetentionScanner implements IScheduledScanner {

    static final int BATCH = 10_000;

    private final ActivityEventRepository events;
    private final TransactionTemplate transaction;

    public ActivityRetentionScanner(ActivityEventRepository events, PlatformTransactionManager transactions) {
        this.events = events;
        this.transaction = new TransactionTemplate(transactions);
    }

    @Override
    public String name() {
        return "activity-retention";
    }

    @Override
    public void scan(Instant now) {
        Instant cutoff = now.minus(ActivityService.RETENTION);
        transaction.executeWithoutResult(status -> events.deleteOlderThan(cutoff, BATCH));
    }
}
