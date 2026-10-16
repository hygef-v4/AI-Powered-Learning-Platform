package edu.aiplatform.backup.application;

import edu.aiplatform.backup.domain.BackupRun;
import edu.aiplatform.backup.domain.BackupRunRepository;
import edu.aiplatform.files.application.StorageUnavailableException;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
