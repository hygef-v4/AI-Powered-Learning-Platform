package edu.aiplatform.backup.service;

import edu.aiplatform.backup.model.BackupRun;
import edu.aiplatform.backup.repository.BackupRunRepository;
import edu.aiplatform.backup.repository.WalSegmentRepository;
import edu.aiplatform.files.service.StorageUnavailableException;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * B3: giữ các base backup thành công trong 7 ngày; không bao giờ xóa bản thành công mới nhất (BR-U03-96). Tệp WAL tải
 * lên trước base backup cũ nhất còn giữ (lùi thêm 1 giờ cho an toàn) không còn dùng để khôi phục nên cũng bị xóa.
 */
@Service
public class BackupRetentionService {

    public static final Duration KEEP = Duration.ofDays(7);
    static final Duration WAL_MARGIN = Duration.ofHours(1);

    private static final Logger log = LoggerFactory.getLogger(BackupRetentionService.class);

    private final BackupRunRepository runs;
    private final IBackupStore store;
    private final WalSegmentRepository walSegments;

    public BackupRetentionService(BackupRunRepository runs, IBackupStore store, WalSegmentRepository walSegments) {
        this.runs = runs;
        this.store = store;
        this.walSegments = walSegments;
    }

    // afterCommit callbacks still have the completed transaction's connection bound. Suspend it so each
    // successful external deletion's metadata update commits, retaining the existing per-archive behavior.
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void purgeOld(Instant now) {
        for (BackupRun run : runs.findPurgeCandidates(now.minus(KEEP))) {
            try {
                store.deleteArchive(run.id().toString());
                runs.markPurged(run.id(), now);
            } catch (StorageUnavailableException e) {
                log.warn("Chưa xóa được bản backup {}, thử lại ở lần chạy sau", run.scheduledFor());
            }
        }
        purgeOldWal(now);
    }

    private void purgeOldWal(Instant now) {
        Instant oldest = runs.findOldestRetainedSnapshot().orElse(null);
        if (oldest == null) {
            return;
        }
        for (WalSegmentRepository.WalSegment segment : walSegments.findPurgeCandidates(oldest.minus(WAL_MARGIN))) {
            try {
                store.deleteWal(segment.archiveRef());
                walSegments.markPurged(segment.fileName(), now);
            } catch (StorageUnavailableException e) {
                log.warn("Chưa xóa được tệp WAL cũ, thử lại ở lần chạy sau");
                return;
            }
        }
    }
}
