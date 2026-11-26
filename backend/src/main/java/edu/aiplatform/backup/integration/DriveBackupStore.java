package edu.aiplatform.backup.integration;

import com.google.api.client.http.FileContent;
import com.google.api.services.drive.model.File;
import edu.aiplatform.backup.service.IBackupStore;
import edu.aiplatform.files.integration.DriveClientFactory;
import edu.aiplatform.files.integration.DriveFolders;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Thư mục backup riêng trên Drive: {@code GOOGLE_BACKUP_FOLDER_ID} với Shared Drive, thư mục {@code backups} trong
 * thư mục app với Drive cá nhân. Mỗi lần chạy một thư mục con tên {@code runId}; tệp WAL nằm trong thư mục con
 * {@code wal}.
 */
public class DriveBackupStore implements IBackupStore {

    public static final String WAL_FOLDER = "wal";

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
    public boolean isAvailable() {
        return true;
    }

    @Override
    public StoredArchive putArchive(UUID runId, Path archive) {
        String parent = folderId.get();
        String runFolder = folders.findFolder(parent, runId.toString())
                .orElseGet(() -> folders.createFolder(parent, runId.toString()));
        File metadata = new File().setName("archive.alapbk").setParents(List.of(runFolder));
        File created = DriveClientFactory.runWithRetry(() -> clients.createDrive().files()
                .create(metadata, new FileContent("application/octet-stream", archive.toFile()))
                .setSupportsAllDrives(true).setFields("id,size,md5Checksum").execute());
        return new StoredArchive(created.getId(), created.getSize() == null ? -1 : created.getSize(),
                created.getMd5Checksum());
    }

    @Override
    public StoredArchive putWal(String name, Path file) {
        String walFolder = folders.ensureChild(folderId.get(), WAL_FOLDER);
        File metadata = new File().setName(name).setParents(List.of(walFolder));
        File created = DriveClientFactory.runWithRetry(() -> clients.createDrive().files()
                .create(metadata, new FileContent("application/octet-stream", file.toFile()))
                .setSupportsAllDrives(true).setFields("id,size,md5Checksum").execute());
        return new StoredArchive(created.getId(), created.getSize() == null ? -1 : created.getSize(),
                created.getMd5Checksum());
    }

    @Override
    public void deleteWal(String ref) {
        try {
            DriveClientFactory.runWithRetry(() -> clients.createDrive().files().delete(ref).setSupportsAllDrives(true)
                    .execute());
        } catch (DriveClientFactory.DriveNotFoundException e) {
            // Đã xóa từ trước.
        }
    }

    @Override
    public void deleteArchive(String runId) {
        Optional<String> folder = folders.findFolder(folderId.get(), runId);
        if (folder.isEmpty()) {
            return;
        }
        try {
            DriveClientFactory.runWithRetry(() -> clients.createDrive().files().delete(folder.get()).setSupportsAllDrives(true)
                    .execute());
        } catch (DriveClientFactory.DriveNotFoundException e) {
            // Đã xóa từ trước.
        }
    }
}
