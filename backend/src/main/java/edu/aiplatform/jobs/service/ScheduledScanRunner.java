package edu.aiplatform.jobs.service;

import edu.aiplatform.jobs.contract.IScheduledScanner;
import java.time.Clock;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * J4: mỗi {@code U03_SCAN_DELAY_MS} (mặc định 15 giây, tính từ khi lượt trước xong) gọi lần lượt các scanner; một scanner
 * lỗi không chặn scanner khác (BR-U03-60). 60 giây làm bài đóng trễ tới 73 giây (TC-PERF-020).
 */
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

    @Scheduled(fixedDelayString = "${U03_SCAN_DELAY_MS:15000}", initialDelay = 30_000)
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
