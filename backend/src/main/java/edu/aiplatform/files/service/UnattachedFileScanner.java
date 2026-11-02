package edu.aiplatform.files.service;

import edu.aiplatform.files.integration.UnattachedFiles;
import edu.aiplatform.jobs.contract.IScheduledScanner;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Mỗi phút trong worker xóa khỏi kho lưu trữ các tệp tải lên mà FileRef đã hết hạn và chưa từng được gắn. */
@Component
public class UnattachedFileScanner implements IScheduledScanner {

    static final int BATCH = 100;
    private static final Logger log = LoggerFactory.getLogger(UnattachedFileScanner.class);

    private final UnattachedFiles unattached;
    private final IStorage storage;

    public UnattachedFileScanner(UnattachedFiles unattached, IStorage storage) {
        this.unattached = unattached;
        this.storage = storage;
    }

    @Override
    public String name() {
        return "unattached-files";
    }

    @Override
    public void scan(Instant now) {
        for (String fileId : unattached.due(now, BATCH)) {
            try {
                storage.delete(fileId);
                unattached.forget(fileId);
            } catch (StorageUnavailableException e) {
                log.warn("Chưa xóa được tệp không dùng; thử lại ở lượt sau");
            }
        }
    }
}
