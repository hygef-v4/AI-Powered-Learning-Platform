package edu.aiplatform.backup.application;

import edu.aiplatform.backup.contract.BackupRunView;
import edu.aiplatform.backup.contract.IBackupStatus;
import edu.aiplatform.backup.contract.IBackupDashboardStatus;
import edu.aiplatform.backup.contract.BackupDashboardRunView;
import edu.aiplatform.backup.contract.BackupDashboardSummary;
import edu.aiplatform.backup.contract.BackupState;
import edu.aiplatform.shared.error.ApiException;
import java.time.Instant;
import edu.aiplatform.backup.domain.BackupRun;
import edu.aiplatform.backup.domain.BackupRunRepository;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cài {@link IBackupStatus} cho Admin Dashboard (U15, B5); không có dữ liệu tải về. */
@Service
public class BackupStatusService implements IBackupStatus, IBackupDashboardStatus {

    private final BackupRunRepository runs;

    public BackupStatusService(BackupRunRepository runs) {
        this.runs = runs;
    }

    @Override
    public Optional<BackupRunView> latest() {
        return runs.latest().map(BackupRun::view);
    }

    @Override
    public Page<BackupRunView> list(Pageable page) {
        return runs.page(page).map(BackupRun::view);
    }

    @Override
    @Transactional(readOnly = true, timeout = 1)
    public Optional<BackupDashboardRunView> latestRun() {
        return runs.latest().map(this::dashboardView);
    }

    @Override
    @Transactional(readOnly = true, timeout = 1)
    public Page<BackupDashboardRunView> history(Pageable page) {
        return runs.page(page).map(this::dashboardView);
    }

    @Override
    @Transactional(readOnly = true, timeout = 1)
    public BackupDashboardSummary summarize(Instant fromInclusive, Instant toExclusive) {
        if (fromInclusive == null || toExclusive == null || !fromInclusive.isBefore(toExclusive)) {
            throw new IllegalArgumentException("Invalid backup summary window");
        }
        var summary = runs.summarize(fromInclusive, toExclusive);
        if (summary.invalidTerminal()) {
            throw ApiException.unavailable("BACKUP_STATUS_UNAVAILABLE", "Không đọc được trạng thái backup");
        }
        return new BackupDashboardSummary(fromInclusive, toExclusive, summary.latestSuccessfulAt(),
                summary.failedRuns(), summary.failedRuns() == 0 ? null : publicFailure(summary.latestFailureMessage()));
    }

    private BackupDashboardRunView dashboardView(BackupRun run) {
        return new BackupDashboardRunView(run.scheduledFor(), run.state(), run.snapshotAt(), run.startedAt(),
                run.completedAt(), run.byteSize(), run.state() == BackupState.FAILED ? publicFailure(run.safeError()) : null,
                run.purgedAt());
    }

    private String publicFailure(String storedError) {
        if ("Quá thời gian xử lý".equals(storedError)
                || "Hết lượt thử lại: Quá thời gian xử lý".equals(storedError)) {
            return "Backup quá thời gian xử lý.";
        }
        return "Backup thất bại. Vui lòng liên hệ người vận hành.";
    }
}
