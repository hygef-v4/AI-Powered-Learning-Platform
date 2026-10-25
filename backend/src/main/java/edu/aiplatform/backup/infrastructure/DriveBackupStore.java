package edu.aiplatform.backup.infrastructure;

import com.google.api.client.http.FileContent;
import com.google.api.services.drive.model.File;
import edu.aiplatform.backup.application.IBackupStore;
import edu.aiplatform.files.infrastructure.DriveClientFactory;
import edu.aiplatform.files.infrastructure.DriveFolders;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Thư mục backup riêng trên Drive: {@code GOOGLE_BACKUP_FOLDER_ID} với Shared Drive, thư mục {@code backups} trong
 * thư mục app với Drive cá nhân. Mỗi lần chạy một thư mục con tên {@code runId}.
 */
public class DriveBackupStore implements IBackupStore {

    private final DriveClientFactory clients;
    private final DriveFolders folders;
    private final Supplier<String> folderId;

    /** {@code folderId} đọc khi cần để lúc khởi động không gọi Drive. */
    public DriveBackupStore(DriveClientFactory clients, DriveFolders folders, Supplier<String> folderId) {
        this.clients = clients;
        this.folders = folders;
        this.folderId = folderId;
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public StoredArchive put(UUID runId, Path archive) {
        String parent = folderId.get();
        String runFolder = folders.find(parent, runId.toString())
                .orElseGet(() -> folders.create(parent, runId.toString()));
        File metadata = new File().setName("archive.alapbk").setParents(List.of(runFolder));
        File created = DriveClientFactory.withRetry(() -> clients.drive().files()
                .create(metadata, new FileContent("application/octet-stream", archive.toFile()))
                .setSupportsAllDrives(true).setFields("id,size,md5Checksum").execute());
        return new StoredArchive(created.getId(), created.getSize() == null ? -1 : created.getSize(),
                created.getMd5Checksum());
    }

    @Override
    public void delete(String runId) {
        Optional<String> folder = folders.find(folderId.get(), runId);
        if (folder.isEmpty()) {
            return;
        }
        try {
            DriveClientFactory.withRetry(() -> clients.drive().files().delete(folder.get()).setSupportsAllDrives(true)
                    .execute());
        } catch (DriveClientFactory.DriveNotFoundException e) {
            // Đã xóa từ trước.
        }
    }
}
