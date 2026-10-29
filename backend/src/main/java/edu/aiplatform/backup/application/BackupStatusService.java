package edu.aiplatform.backup.application;

import edu.aiplatform.backup.contract.BackupRunView;
import edu.aiplatform.backup.contract.IBackupStatus;
import edu.aiplatform.backup.domain.BackupRun;
import edu.aiplatform.backup.domain.BackupRunRepository;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/** Cài {@link IBackupStatus} cho Admin Dashboard (U15, B5); không có dữ liệu tải về. */
@Service
public class BackupStatusService implements IBackupStatus {

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
}
