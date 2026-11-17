package edu.aiplatform.files.integration;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import edu.aiplatform.files.config.FilesProperties;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Thư mục trên Drive cho cả hai cách cấu hình. Gốc là Shared Drive, hoặc thư mục
 * {@link FilesProperties#rootFolderName()} app tự tạo trong My Drive (scope {@code drive.file} chỉ thấy tệp do app tạo nên app phải tự tạo thư mục gốc).
 */
@Component
public class DriveFolders {

    public static final String FOLDER_MIME = "application/vnd.google-apps.folder";
    private static final String ROOT = "";
    private static final Logger log = LoggerFactory.getLogger(DriveFolders.class);

    private final DriveClientFactory clients;
    private final FilesProperties properties;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public DriveFolders(DriveClientFactory clients, FilesProperties properties) {
        this.clients = clients;
        this.properties = properties;
    }

    /** Thư mục gốc chứa mọi thư mục của app. */
    public String resolveRoot() {
        if (properties.hasSharedDrive()) {
            return properties.sharedDriveId();
        }
        return cache.computeIfAbsent(ROOT, key -> findConfiguredRoot().orElseGet(() ->
                findFolder("root", properties.resolveRootFolderName()).orElseGet(() -> createFolder("root", properties.resolveRootFolderName()))));
    }

    /**
     * {@code GOOGLE_DRIVE_FOLDER_ID} đặt thì dùng thẳng id đó; id không truy cập được (thư mục tạo tay, đã xóa,
     * hoặc của app khác) thì cảnh báo rồi quay về tìm hoặc tạo theo tên.
     */
    private Optional<String> findConfiguredRoot() {
        String id = properties.driveFolderId();
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        try {
            DriveClientFactory.runWithRetry(() -> clients.createDrive().files().get(id.strip()).setFields("id").execute());
            return Optional.of(id.strip());
        } catch (DriveClientFactory.DriveNotFoundException e) {
            log.warn("GOOGLE_DRIVE_FOLDER_ID không truy cập được (scope drive.file chỉ thấy thư mục do app tạo); "
                    + "dùng thư mục theo tên");
            return Optional.empty();
        }
    }

    /** Thư mục con cố định (theo purpose, thư mục backup), nhớ id sau lần đầu. */
    public String ensureChild(String parentId, String name) {
        return cache.computeIfAbsent(parentId + "/" + name,
                key -> findFolder(parentId, name).orElseGet(() -> createFolder(parentId, name)));
    }

    /** Tên thư mục gốc lấy từ cấu hình nên thoát {@code \} và {@code '} trước khi ghép vào câu truy vấn. */
    public Optional<String> findFolder(String parentId, String name) {
        String escaped = name.replace("\\", "\\\\").replace("'", "\\'");
        String query = "'" + parentId + "' in parents and name = '" + escaped + "' and mimeType = '" + FOLDER_MIME
                + "' and trashed = false";
        FileList found = DriveClientFactory.runWithRetry(() -> listFiles(query).execute());
        return found.getFiles() == null || found.getFiles().isEmpty() ? Optional.empty()
                : Optional.of(found.getFiles().get(0).getId());
    }

    public String createFolder(String parentId, String name) {
        File folder = new File().setName(name).setMimeType(FOLDER_MIME).setParents(List.of(parentId));
        return DriveClientFactory.runWithRetry(() -> clients.createDrive().files().create(folder).setSupportsAllDrives(true)
                .setFields("id").execute()).getId();
    }

    private Drive.Files.List listFiles(String query) throws IOException {
        Drive.Files.List list = clients.createDrive().files().list().setQ(query).setFields("files(id)");
        if (properties.hasSharedDrive()) {
            list.setCorpora("drive").setDriveId(properties.sharedDriveId()).setIncludeItemsFromAllDrives(true)
                    .setSupportsAllDrives(true);
        }
        return list;
    }
}
