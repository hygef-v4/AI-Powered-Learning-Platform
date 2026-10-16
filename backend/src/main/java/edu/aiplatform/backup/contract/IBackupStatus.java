package edu.aiplatform.backup.contract;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Trạng thái và lịch sử backup cho Admin Dashboard (U15). */
public interface IBackupStatus {

    Optional<BackupRunView> latest();

    Page<BackupRunView> list(Pageable page);
}
