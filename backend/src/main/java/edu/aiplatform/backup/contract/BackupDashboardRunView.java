package edu.aiplatform.backup.contract;

import java.time.Instant;
import java.time.LocalDate;

public record BackupDashboardRunView(LocalDate scheduledFor, BackupState state, Instant snapshotAt,
        Instant startedAt, Instant completedAt, Long byteSize, String safeError, Instant purgedAt) { }
