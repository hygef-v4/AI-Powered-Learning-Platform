package edu.aiplatform.files.integration;

import edu.aiplatform.files.config.FilesProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/** Health Drive (P7): gọi {@code drives.get} có cache 60 giây; lỗi chỉ báo DEGRADED, backend vẫn UP. */
@Component("drive")
public class DriveHealthIndicator implements HealthIndicator {

    static final Duration CACHE = Duration.ofSeconds(60);

    private final FilesProperties properties;
    private final DriveClientFactory clients;
    private final Clock clock;
    private volatile Instant checkedAt = Instant.EPOCH;
    private volatile String state = "UNKNOWN";

    public DriveHealthIndicator(FilesProperties properties, DriveClientFactory clients, Clock clock) {
        this.properties = properties;
        this.clients = clients;
        this.clock = clock;
    }

    @Override
    public Health health() {
        if (!properties.driveConfigured()) {
            return Health.up().withDetail("drive", properties.requireDrive() ? "NOT_CONFIGURED" : "LOCAL").build();
        }
        Instant now = clock.instant();
        if (now.isAfter(checkedAt.plus(CACHE))) {
            try {
                DriveClientFactory.withRetry(() -> clients.drive().drives().get(properties.sharedDriveId())
                        .setFields("id").execute());
                state = "UP";
            } catch (RuntimeException e) {
                state = "DEGRADED";
            }
            checkedAt = now;
        }
        return Health.up().withDetail("drive", state).build();
    }
}
