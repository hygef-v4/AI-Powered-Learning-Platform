package edu.aiplatform.backup.infrastructure;

import com.google.api.client.http.FileContent;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import edu.aiplatform.backup.application.IBackupStore;
import edu.aiplatform.files.infrastructure.DriveClientFactory;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Thư mục backup riêng trên Drive ({@code GOOGLE_BACKUP_FOLDER_ID}): mỗi lần chạy một thư mục con tên {@code runId}. */
public class DriveBackupStore implements IBackupStore {

    static final String FOLDER_MIME = "application/vnd.google-apps.folder";

    private final DriveClientFactory clients;
    private final String folderId;

    public DriveBackupStore(DriveClientFactory clients, String folderId) {
        this.clients = clients;
        this.folderId = folderId;
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public StoredArchive put(UUID runId, Path archive) {
        String runFolder = findFolder(runId.toString()).orElseGet(() -> createFolder(runId.toString()));
        File metadata = new File().setName("archive.alapbk").setParents(List.of(runFolder));
        File created = DriveClientFactory.withRetry(() -> clients.drive().files()
                .create(metadata, new FileContent("application/octet-stream", archive.toFile()))
                .setSupportsAllDrives(true).setFields("id,size,md5Checksum").execute());
        return new StoredArchive(created.getId(), created.getSize() == null ? -1 : created.getSize(),
                created.getMd5Checksum());
    }

    @Override
    public void delete(String runId) {
        Optional<String> folder = findFolder(runId);
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

    private Optional<String> findFolder(String name) {
        String query = "'" + folderId + "' in parents and name = '" + name + "' and mimeType = '" + FOLDER_MIME
                + "' and trashed = false";
        FileList found = DriveClientFactory.withRetry(() -> clients.drive().files().list().setQ(query)
                .setIncludeItemsFromAllDrives(true).setSupportsAllDrives(true).setFields("files(id)").execute());
        return found.getFiles() == null || found.getFiles().isEmpty() ? Optional.empty()
                : Optional.of(found.getFiles().get(0).getId());
    }

    private String createFolder(String name) {
        File folder = new File().setName(name).setMimeType(FOLDER_MIME).setParents(List.of(folderId));
        return DriveClientFactory.withRetry(() -> clients.drive().files().create(folder).setSupportsAllDrives(true)
                .setFields("id").execute()).getId();
    }
}
