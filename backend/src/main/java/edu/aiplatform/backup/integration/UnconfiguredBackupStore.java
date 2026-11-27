package edu.aiplatform.backup.integration;

import edu.aiplatform.backup.service.IBackupStore;
import edu.aiplatform.files.service.StorageUnavailableException;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Chưa cấu hình Google Drive: lịch backup không tạo lần chạy, worker không đẩy WAL; lần chạy cũ còn dở thì lỗi tạm
 * rồi {@code FAILED}.
 */
public class UnconfiguredBackupStore implements IBackupStore {

    private static final String MESSAGE = "Chưa cấu hình Google Drive cho backup";

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public StoredArchive putArchive(UUID runId, Path archive) {
        throw new StorageUnavailableException(MESSAGE, null);
    }

    @Override
    public void deleteArchive(String runId) {
        throw new StorageUnavailableException(MESSAGE, null);
    }

    @Override
    public StoredArchive putWal(String name, Path file) {
        throw new StorageUnavailableException(MESSAGE, null);
    }

    @Override
    public void deleteWal(String ref) {
        throw new StorageUnavailableException(MESSAGE, null);
    }
}
