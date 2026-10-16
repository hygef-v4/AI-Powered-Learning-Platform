package edu.aiplatform.backup.infrastructure;

import edu.aiplatform.backup.application.IBackupStore;
import edu.aiplatform.files.application.StorageUnavailableException;
import java.nio.file.Path;
import java.util.UUID;

/** Production thiếu thư mục backup trên Drive: lần chạy lỗi tạm, hết lượt thì {@code FAILED} và báo Admin. */
public class UnconfiguredBackupStore implements IBackupStore {

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public StoredArchive put(UUID runId, Path archive) {
        throw new StorageUnavailableException("Chưa cấu hình thư mục backup trên Drive", null);
    }

    @Override
    public void delete(String runId) {
        throw new StorageUnavailableException("Chưa cấu hình thư mục backup trên Drive", null);
    }
}
