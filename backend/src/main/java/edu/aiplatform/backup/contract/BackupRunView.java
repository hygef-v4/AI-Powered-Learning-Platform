package edu.aiplatform.backup.contract;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Trạng thái một lần backup database; không có link hay dữ liệu tải về. */
public record BackupRunView(UUID id, LocalDate scheduledFor, BackupState state, Instant snapshotAt,
                            Instant completedAt, Long byteSize, String safeError, Instant purgedAt) {
}
