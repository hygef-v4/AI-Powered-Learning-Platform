package edu.aiplatform.assignmentwork.application;

import edu.aiplatform.assignmentwork.domain.ActivityEventRepository;
import edu.aiplatform.jobs.contract.IScheduledScanner;
import edu.aiplatform.settings.contract.ISettings;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** F12 bước 4 (BR-U08-54; P5): xóa sự kiện quá {@code activity.retentionDays}, tối đa 10.000 mỗi lần chạy. */
@Component
public class ActivityRetentionScanner implements IScheduledScanner {

    static final int BATCH = 10_000;

    private final ActivityEventRepository events;
    private final ISettings settings;
    private final TransactionTemplate transaction;

    public ActivityRetentionScanner(ActivityEventRepository events, ISettings settings,
                                    PlatformTransactionManager transactions) {
        this.events = events;
        this.settings = settings;
        this.transaction = new TransactionTemplate(transactions);
    }

    @Override
    public String name() {
        return "activity-retention";
    }

    @Override
    public void scan(Instant now) {
        Optional<Integer> days = settings.get(ActivityService.RETENTION_DAYS);
        if (days.isEmpty() || days.get() < 1) {
            return;
        }
        Instant cutoff = now.minus(Duration.ofDays(days.get()));
        transaction.executeWithoutResult(status -> events.deleteOlderThan(cutoff, BATCH));
    }
}
