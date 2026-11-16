package edu.aiplatform.files.integration;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.model.File;
import edu.aiplatform.files.service.FileAbusiveException;
import edu.aiplatform.files.service.IStorage;
import edu.aiplatform.files.service.StorageUnavailableException;
import edu.aiplatform.files.service.StoredMetadata;
import edu.aiplatform.files.contract.ArtifactPurpose;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Lưu tệp trên Google Drive (Shared Drive hoặc Drive cá nhân), mỗi {@code purpose} một thư mục tạo khi cần (NFR-U03-11). */
public class GoogleDriveStorageAdapter implements IStorage {

    private final DriveClientFactory clients;
    private final DriveFolders folders;

    public GoogleDriveStorageAdapter(DriveClientFactory clients, DriveFolders folders) {
        this.clients = clients;
        this.folders = folders;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String putFile(ArtifactPurpose purpose, Path content, StoredMetadata metadata) {
        File file = new File()
                .setName(metadata.originalFileName())
                .setParents(List.of(resolveFolder(purpose)))
                .setAppProperties(Map.of(
                        "ownerAccountId", metadata.ownerAccountId().toString(),
                        "purpose", purpose.name(),
                        "mediaType", metadata.mediaType(),
                        "sha256", metadata.sha256()));
        File created = DriveClientFactory.runWithRetry(() -> clients.createDrive().files()
                .create(file, new FileContent(metadata.mediaType(), content.toFile()))
                .setSupportsAllDrives(true).setFields("id").execute());
        return created.getId();
    }

    @Override
    public Optional<StoredMetadata> loadMetadata(String fileId) {
        try {
            File file = DriveClientFactory.runWithRetry(() -> clients.createDrive().files().get(fileId)
                    .setSupportsAllDrives(true).setFields("id,name,size,appProperties").execute());
            Map<String, String> props = file.getAppProperties() == null ? Map.of() : file.getAppProperties();
            return Optional.of(new StoredMetadata(UUID.fromString(props.get("ownerAccountId")),
                    ArtifactPurpose.valueOf(props.get("purpose")), file.getName(), props.get("mediaType"),
                    file.getSize() == null ? 0 : file.getSize(), props.get("sha256")));
        } catch (DriveClientFactory.DriveNotFoundException e) {
            return Optional.empty();
        }
    }

    @Override
    public InputStream open(String fileId) {
        try {
            return clients.createDrive().files().get(fileId).setSupportsAllDrives(true).executeMediaAsInputStream();
        } catch (GoogleJsonResponseException e) {
            if (isAbuse(e)) {
                throw new FileAbusiveException(fileId);
            }
            throw new StorageUnavailableException("Drive trả lỗi " + e.getStatusCode(), e);
        } catch (IOException e) {
            throw new StorageUnavailableException("Không đọc được tệp từ Drive", e);
        }
    }

    @Override
    public void deleteFile(String fileId) {
        try {
            DriveClientFactory.runWithRetry(() -> clients.createDrive().files().delete(fileId).setSupportsAllDrives(true)
                    .execute());
        } catch (DriveClientFactory.DriveNotFoundException e) {
            // Đã xóa từ trước.
        }
    }

    private String resolveFolder(ArtifactPurpose purpose) {
        return folders.ensureChild(folders.resolveRoot(), purpose.name());
    }

    static boolean isAbuse(GoogleJsonResponseException e) {
        GoogleJsonError details = e.getDetails();
        return e.getStatusCode() == 403 && details != null && details.getErrors() != null
                && details.getErrors().stream().anyMatch(error -> "cannotDownloadAbusiveFile".equals(error.getReason()));
    }
}
