package edu.aiplatform.backup.service;

import edu.aiplatform.backup.model.BackupRun;
import edu.aiplatform.backup.repository.BackupRunRepository;
import edu.aiplatform.files.service.StorageUnavailableException;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** B3: giữ các bản thành công trong 7 ngày; không bao giờ xóa bản thành công mới nhất (BR-U03-96). */
@Service
public class BackupRetentionService {

    public static final Duration KEEP = Duration.ofDays(7);

    private static final Logger log = LoggerFactory.getLogger(BackupRetentionService.class);

    private final BackupRunRepository runs;
    private final IBackupStore store;

    public BackupRetentionService(BackupRunRepository runs, IBackupStore store) {
        this.runs = runs;
        this.store = store;
    }

    // afterCommit callbacks still have the completed transaction's connection bound. Suspend it so each
    // successful external deletion's metadata update commits, retaining the existing per-archive behavior.
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void purgeOld(Instant now) {
        for (BackupRun run : runs.findPurgeCandidates(now.minus(KEEP))) {
            try {
                store.delete(run.id().toString());
                runs.markPurged(run.id(), now);
            } catch (StorageUnavailableException e) {
                log.warn("Chưa xóa được bản backup {}, thử lại ở lần chạy sau", run.scheduledFor());
            }
        }
    }
}
