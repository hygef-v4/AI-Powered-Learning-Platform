package edu.aiplatform.backup.contract;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Read-only dashboard metadata; the legacy backup port remains unchanged. */
public interface IBackupDashboardStatus {
    Optional<BackupDashboardRunView> latestRun();
    Page<BackupDashboardRunView> history(Pageable page);
    BackupDashboardSummary summarize(Instant fromInclusive, Instant toExclusive);
}
