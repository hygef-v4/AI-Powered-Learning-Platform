package edu.aiplatform.files.infrastructure;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * Health của Google Drive (Bước 17, P7): gọi `drives.get` có cache 60 s. Drive lỗi trả `DEGRADED` (HTTP 200) nên
 * backend không bị coi là `DOWN`; nhóm `/health/ready` không gồm Drive.
 */
@Component("drive")
@ConditionalOnExpression("!'${platform.files.drive.service-account-key:}'.isBlank()")
public class DriveHealthIndicator implements HealthIndicator {

    public static final Status DEGRADED = new Status("DEGRADED", "Google Drive is unavailable");
    static final Duration CACHE = Duration.ofSeconds(60);

    private final GoogleDriveStorageAdapter drive;
    private final Clock clock;
    private volatile Health cached;
    private volatile Instant cachedAt = Instant.EPOCH;

    @Autowired
    public DriveHealthIndicator(GoogleDriveStorageAdapter drive) {
        this(drive, Clock.systemUTC());
    }

    DriveHealthIndicator(GoogleDriveStorageAdapter drive, Clock clock) {
        this.drive = drive;
        this.clock = clock;
    }

    @Override
    public Health health() {
        Instant now = clock.instant();
        Health current = cached;
        if (current != null && now.isBefore(cachedAt.plus(CACHE))) {
            return current;
        }
        Health fresh;
        try {
            drive.checkSharedDrive();
            fresh = Health.up().build();
        } catch (RuntimeException e) {
            fresh = Health.status(DEGRADED).withDetail("error", e.getClass().getSimpleName()).build();
        }
        cached = fresh;
        cachedAt = now;
        return fresh;
    }
}
