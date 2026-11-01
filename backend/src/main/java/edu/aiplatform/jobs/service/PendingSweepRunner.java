package edu.aiplatform.jobs.service;

import edu.aiplatform.jobs.contract.IPendingSweeper;
import edu.aiplatform.jobs.contract.PendingJob;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * J3: mỗi phút gửi lại việc còn chờ quá 5 phút (BR-U03-58). Chỉ chạy trong worker. Một việc vừa được gửi lại thì chờ
 * {@link #RESEND_INTERVAL} mới gửi lần nữa: việc còn chờ lâu thường đang nằm ở hàng thử lại (tổng backoff 15,5 phút)
 * hoặc đang chạy, gửi mỗi phút chỉ tạo thêm bản trùng.
 */
@Component
@Profile("worker")
public class PendingSweepRunner {

    static final Duration STALE_AFTER = Duration.ofMinutes(5);
    static final Duration RESEND_INTERVAL = Duration.ofMinutes(20);

    private static final Logger log = LoggerFactory.getLogger(PendingSweepRunner.class);

    private final List<IPendingSweeper> sweepers;
    private final JobPublisher publisher;
    private final Clock clock;
    private final Map<String, Instant> lastResent = new ConcurrentHashMap<>();

    public PendingSweepRunner(List<IPendingSweeper> sweepers, JobPublisher publisher, Clock clock) {
        this.sweepers = sweepers;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void runOnce() {
        Instant now = clock.instant();
        Instant olderThan = now.minus(STALE_AFTER);
        lastResent.values().removeIf(at -> !at.plus(RESEND_INTERVAL).isAfter(now));
        for (IPendingSweeper sweeper : sweepers) {
            try {
                for (PendingJob pending : sweeper.findStalePending(olderThan)) {
                    String key = sweeper.jobType() + ":" + pending.idempotencyKey();
                    if (lastResent.putIfAbsent(key, now) == null) {
                        publisher.resend(sweeper.jobType(), pending);
                    }
                }
            } catch (RuntimeException e) {
                log.error("Sweeper {} lỗi", sweeper.jobType(), e);
            }
        }
    }
}
