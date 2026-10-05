package edu.aiplatform.files.worker;

import edu.aiplatform.files.port.StoragePort;
import edu.aiplatform.files.port.StorageUnavailableException;
import edu.aiplatform.jobs.port.JobHandler;
import edu.aiplatform.jobs.port.JobMessage;
import edu.aiplatform.jobs.port.JobQueue;
import edu.aiplatform.jobs.port.JobTypes;
import edu.aiplatform.jobs.port.RetryableJobException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Việc `DRIVE_CLEANUP` (queue `jobs.drive`): xóa tệp ghi dở khi upload lỗi mà xóa ngay không được (BR-U03-06).
 * Idempotent: tệp không còn thì coi như đã xong. Không có dòng nghiệp vụ nên hết lượt chỉ log ERROR.
 */
@Component
public class DriveJobHandler implements JobHandler {

    private static final Logger log = LoggerFactory.getLogger(DriveJobHandler.class);

    private final StoragePort storage;

    public DriveJobHandler(StoragePort storage) {
        this.storage = storage;
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
        Object fileId = message.payload().get("fileId");
        if (!(fileId instanceof String id) || id.isBlank()) {
            throw new IllegalArgumentException("DRIVE_CLEANUP payload has no fileId");
        }
        try {
            storage.delete(id);
        } catch (StorageUnavailableException e) {
            throw new RetryableJobException("Storage unavailable", e);
        }
    }

    @Override
    public void onFailed(JobMessage message, Throwable lastError) {
        log.error("Orphan file could not be removed (key={}): {}", message.idempotencyKey(), lastError.toString());
    }
}
