package edu.aiplatform.backup.domain;

import edu.aiplatform.backup.contract.BackupRunView;
import edu.aiplatform.backup.contract.BackupState;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Một lần backup (bảng {@code backup_runs}). */
public record BackupRun(UUID id, LocalDate scheduledFor, BackupState state, Instant createdAt, Instant snapshotAt,
                        String schemaVersion, String appVersion, String archiveRef, String archiveSha256,
                        Long byteSize, Instant startedAt, Instant completedAt, Instant purgedAt, String safeError) {

    public BackupRunView view() {
        return new BackupRunView(id, scheduledFor, state, snapshotAt, completedAt, byteSize, safeError, purgedAt);
    }
}
