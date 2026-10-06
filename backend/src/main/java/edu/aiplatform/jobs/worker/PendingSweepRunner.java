package edu.aiplatform.jobs.worker;

import edu.aiplatform.jobs.infrastructure.AmqpPublisher;
import edu.aiplatform.jobs.port.JobMessage;
import edu.aiplatform.jobs.port.PendingSweeper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mỗi phút gọi từng {@link PendingSweeper}: dòng còn chờ quá 5 phút được gửi lại message (J3, BR-U03-58, P10).
 * Handler idempotent nên gửi trùng không gây hại.
 */
public class PendingSweepRunner implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(PendingSweepRunner.class);

    private final JobHandlerRegistry registry;
    private final AmqpPublisher amqp;
    private final Duration republishAfter;
    private final Clock clock;

    public PendingSweepRunner(JobHandlerRegistry registry, AmqpPublisher amqp, Duration republishAfter, Clock clock) {
        this.registry = registry;
        this.amqp = amqp;
        this.republishAfter = republishAfter;
        this.clock = clock;
    }

    @Override
    public void run() {
        Instant olderThan = clock.instant().minus(republishAfter);
        for (PendingSweeper sweeper : registry.sweepers()) {
            try {
                List<JobMessage> stale = sweeper.findStalePending(olderThan);
                stale.forEach(amqp::publishJob);
                if (!stale.isEmpty()) {
                    log.info("Republished {} pending job(s) of type {}", stale.size(), sweeper.jobType());
                }
            } catch (RuntimeException e) {
                log.error("Pending sweeper {} failed: {}", sweeper.jobType(), e.toString());
            }
        }
    }
}
