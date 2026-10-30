package edu.aiplatform.files.service;

import edu.aiplatform.jobs.contract.IJobHandler;
import edu.aiplatform.jobs.contract.JobMessage;
import edu.aiplatform.jobs.contract.JobQueue;
import edu.aiplatform.jobs.contract.JobTypes;
import edu.aiplatform.jobs.contract.RetryableJobException;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * {@code DRIVE_CLEANUP}: xóa tệp dở dang của upload lỗi ({@code storage = FILES}). Phần dọn archive backup
 * ({@code storage = BACKUP}) do package backup cài qua {@link IDriveCleanupTarget}.
 */
@Component
public class DriveCleanupJobHandler implements IJobHandler {

    private static final Logger log = LoggerFactory.getLogger(DriveCleanupJobHandler.class);

    private final IStorage storage;
    private final java.util.List<IDriveCleanupTarget> targets;

    public DriveCleanupJobHandler(IStorage storage, java.util.List<IDriveCleanupTarget> targets) {
        this.storage = storage;
        this.targets = targets;
    }

    @Override
    public String jobType() {
        return JobTypes.DRIVE_CLEANUP;
    }

    @Override
    public JobQueue queue() {
        return JobQueue.DRIVE;
    }

    @Override
    public void handle(JobMessage message) {
        String area = String.valueOf(message.payload().get("storage"));
        String ref = String.valueOf(message.payload().get("ref"));
        try {
            if ("FILES".equals(area)) {
                storage.delete(ref);
                return;
            }
            for (IDriveCleanupTarget target : targets) {
                if (target.area().equals(area)) {
                    target.delete(ref);
                    return;
                }
            }
            log.warn("DRIVE_CLEANUP: không có nơi dọn cho {}", area);
        } catch (StorageUnavailableException e) {
            throw new RetryableJobException("Drive tạm thời không khả dụng", e);
        }
    }

    @Override
    public void onFailed(JobMessage message, String error) {
        log.error("DRIVE_CLEANUP thất bại sau nhiều lần thử: {}", error);
    }

    @Override
    public Duration timeout() {
        return Duration.ofMinutes(2);
    }
}
