package edu.aiplatform.jobs.application;

import edu.aiplatform.jobs.contract.IScheduledScanner;
import java.time.Clock;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** J4: mỗi phút gọi lần lượt các scanner; một scanner lỗi không chặn scanner khác (BR-U03-60). */
@Component
@Profile("worker")
public class ScheduledScanRunner {

    private static final Logger log = LoggerFactory.getLogger(ScheduledScanRunner.class);

    private final List<IScheduledScanner> scanners;
    private final Clock clock;

    public ScheduledScanRunner(List<IScheduledScanner> scanners, Clock clock) {
        this.scanners = scanners;
        this.clock = clock;
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void runOnce() {
        for (IScheduledScanner scanner : scanners) {
            try {
                scanner.scan(clock.instant());
            } catch (RuntimeException e) {
                log.error("Scanner {} lỗi", scanner.name(), e);
            }
        }
    }
}
