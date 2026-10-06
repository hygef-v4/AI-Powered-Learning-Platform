package edu.aiplatform.jobs.application;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Cấu hình việc nền `platform.jobs.*` (biến `U03_JOB_*`, `U03_SWEEP_*`, `U03_SCAN_INTERVAL`). */
@ConfigurationProperties("platform.jobs")
public record JobProperties(
        int maxAttempts,
        List<Duration> backoff,
        Duration sweepInterval,
        Duration sweepRepublishAfter,
        Duration scanInterval,
        Duration handlerTimeout,
        int workerMaxConcurrency) {

    public JobProperties {
        if (backoff == null || backoff.size() != maxAttempts) {
            throw new IllegalArgumentException("platform.jobs.backoff must have exactly max-attempts entries");
        }
        backoff = List.copyOf(backoff);
    }
}
