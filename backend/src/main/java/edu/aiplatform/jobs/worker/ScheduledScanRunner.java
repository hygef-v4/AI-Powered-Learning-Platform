package edu.aiplatform.jobs.worker;

import edu.aiplatform.jobs.port.ScheduledScanner;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mỗi phút gọi lần lượt các {@link ScheduledScanner} (J4, BR-U03-60, P10). Một scanner lỗi không chặn scanner khác;
 * scanner cập nhật có điều kiện nên chạy lại không gây trùng.
 */
public class ScheduledScanRunner implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ScheduledScanRunner.class);

    private final JobHandlerRegistry registry;
    private final Clock clock;

    public ScheduledScanRunner(JobHandlerRegistry registry, Clock clock) {
        this.registry = registry;
        this.clock = clock;
    }

    @Override
    public void run() {
        Instant now = clock.instant();
        for (ScheduledScanner scanner : registry.scanners()) {
            try {
                scanner.scan(now);
            } catch (RuntimeException e) {
                log.error("Scheduled scanner {} failed: {}", scanner.name(), e.toString());
            }
        }
    }
}
