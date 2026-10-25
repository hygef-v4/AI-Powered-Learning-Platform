package edu.aiplatform.files.infrastructure;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import edu.aiplatform.files.application.FilesProperties;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Thư mục trên Drive cho cả hai cách cấu hình. Gốc là Shared Drive, hoặc thư mục {@value #APP_FOLDER} app tự tạo trong
 * My Drive (scope {@code drive.file} chỉ thấy tệp do app tạo nên app phải tự tạo thư mục gốc).
 */
@Component
public class DriveFolders {

    public static final String FOLDER_MIME = "application/vnd.google-apps.folder";
    static final String APP_FOLDER = "AI Learning Platform";
    private static final String ROOT = "";

    private final DriveClientFactory clients;
    private final FilesProperties properties;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public DriveFolders(DriveClientFactory clients, FilesProperties properties) {
        this.clients = clients;
        this.properties = properties;
    }

    /** Thư mục gốc chứa mọi thư mục của app. */
    public String root() {
        if (properties.sharedDriveConfigured()) {
            return properties.sharedDriveId();
        }
        return cache.computeIfAbsent(ROOT, key -> find("root", APP_FOLDER).orElseGet(() -> create("root", APP_FOLDER)));
    }

    /** Thư mục con cố định (theo purpose, thư mục backup), nhớ id sau lần đầu. */
    public String child(String parentId, String name) {
        return cache.computeIfAbsent(parentId + "/" + name,
                key -> find(parentId, name).orElseGet(() -> create(parentId, name)));
    }

    /** Tên thư mục là hằng hoặc UUID nên không cần thoát ký tự trong câu truy vấn. */
    public Optional<String> find(String parentId, String name) {
        String query = "'" + parentId + "' in parents and name = '" + name + "' and mimeType = '" + FOLDER_MIME
                + "' and trashed = false";
        FileList found = DriveClientFactory.withRetry(() -> list(query).execute());
        return found.getFiles() == null || found.getFiles().isEmpty() ? Optional.empty()
                : Optional.of(found.getFiles().get(0).getId());
    }

    public String create(String parentId, String name) {
        File folder = new File().setName(name).setMimeType(FOLDER_MIME).setParents(List.of(parentId));
        return DriveClientFactory.withRetry(() -> clients.drive().files().create(folder).setSupportsAllDrives(true)
                .setFields("id").execute()).getId();
    }

    private Drive.Files.List list(String query) throws IOException {
        Drive.Files.List list = clients.drive().files().list().setQ(query).setFields("files(id)");
        if (properties.sharedDriveConfigured()) {
            list.setCorpora("drive").setDriveId(properties.sharedDriveId()).setIncludeItemsFromAllDrives(true)
                    .setSupportsAllDrives(true);
        }
        return list;
    }
}
