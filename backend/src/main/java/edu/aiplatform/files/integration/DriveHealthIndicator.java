package edu.aiplatform.files.integration;

import edu.aiplatform.files.config.FilesProperties;
import jakarta.annotation.PreDestroy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/** Health Drive trả cache ngay; một probe nền duy nhất, Drive chậm không chặn health của ứng dụng. */
@Component("drive")
public class DriveHealthIndicator implements HealthIndicator {

    static final Duration CACHE = Duration.ofSeconds(60);
    static final Duration PROBE_BUDGET = Duration.ofSeconds(2);

    private final FilesProperties properties;
    private final DriveClientFactory clients;
    private final DriveFolders folders;
    private final Clock clock;
    private volatile Instant checkedAt = Instant.EPOCH;
    private volatile String state = "UNKNOWN";
    private final AtomicBoolean refreshing = new AtomicBoolean();
    private volatile Instant startedAt = Instant.EPOCH;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "drive-health");
        thread.setDaemon(true);
        return thread;
    });

    public DriveHealthIndicator(FilesProperties properties, DriveClientFactory clients, DriveFolders folders,
                                Clock clock) {
        this.properties = properties;
        this.clients = clients;
        this.folders = folders;
        this.clock = clock;
    }

    @Override
    public Health health() {
        if (!properties.hasDrive()) {
            return Health.up().withDetail("drive", properties.requireDrive() ? "NOT_CONFIGURED" : "LOCAL").build();
        }
        Instant now = clock.instant();
        if (now.isAfter(checkedAt.plus(CACHE)) && refreshing.compareAndSet(false, true)) {
            startedAt = now;
            try {
                executor.execute(this::refreshHealth);
            } catch (RuntimeException rejected) {
                state = "DEGRADED";
                checkedAt = now;
                refreshing.set(false);
            }
        }
        String currentState = refreshing.get() && now.isAfter(startedAt.plus(PROBE_BUDGET))
                ? "DEGRADED" : state;
        return Health.up().withDetail("drive", currentState).build();
    }

    private void refreshHealth() {
        try {
            if (properties.hasSharedDrive()) {
                DriveClientFactory.runWithRetry(() -> clients.createDrive().drives().get(properties.sharedDriveId())
                        .setFields("id").execute());
            } else {
                String root = folders.resolveRoot();
                DriveClientFactory.runWithRetry(() -> clients.createDrive().files().get(root).setFields("id").execute());
            }
            state = clock.instant().isAfter(startedAt.plus(PROBE_BUDGET)) ? "DEGRADED" : "UP";
        } catch (RuntimeException e) {
            state = "DEGRADED";
        } finally {
            checkedAt = clock.instant();
            // Keep this permit until the call actually ends, even if it ignores interruption.
            refreshing.set(false);
        }
    }

    @PreDestroy
    public void closeProbe() {
        executor.shutdownNow();
    }
}
