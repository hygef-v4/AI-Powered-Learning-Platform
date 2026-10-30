package edu.aiplatform.jobs.service;

import edu.aiplatform.jobs.contract.IPendingSweeper;
import edu.aiplatform.jobs.contract.PendingJob;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** J3: mỗi phút gửi lại việc còn chờ quá 5 phút (BR-U03-58). Chỉ chạy trong worker. */
@Component
@Profile("worker")
public class PendingSweepRunner {

    static final Duration STALE_AFTER = Duration.ofMinutes(5);

    private static final Logger log = LoggerFactory.getLogger(PendingSweepRunner.class);

    private final List<IPendingSweeper> sweepers;
    private final JobPublisher publisher;
    private final Clock clock;

    public PendingSweepRunner(List<IPendingSweeper> sweepers, JobPublisher publisher, Clock clock) {
        this.sweepers = sweepers;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void runOnce() {
        Instant olderThan = clock.instant().minus(STALE_AFTER);
        for (IPendingSweeper sweeper : sweepers) {
            try {
                for (PendingJob pending : sweeper.findStalePending(olderThan)) {
                    publisher.resend(sweeper.jobType(), pending);
                }
            } catch (RuntimeException e) {
                log.error("Sweeper {} lỗi", sweeper.jobType(), e);
            }
        }
    }
}
