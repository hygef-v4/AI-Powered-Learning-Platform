package edu.aiplatform.files.infrastructure;

import edu.aiplatform.files.application.FilesProperties;
import edu.aiplatform.files.application.IStorage;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Chọn nơi lưu tệp (P5): có key Drive thì Drive; production bắt buộc Drive; còn lại thư mục local. */
@Configuration
@EnableConfigurationProperties(FilesProperties.class)
public class StorageConfig {

    private static final Logger log = LoggerFactory.getLogger(StorageConfig.class);

    @Bean
    IStorage storage(FilesProperties properties, DriveClientFactory driveClients, DriveFolders folders) {
        if (properties.driveConfigured()) {
            log.info("Lưu tệp trên Google Drive ({})",
                    properties.sharedDriveConfigured() ? "Shared Drive" : "Drive cá nhân");
            return new GoogleDriveStorageAdapter(driveClients, folders);
        }
        if (properties.requireDrive()) {
            log.warn("Chưa cấu hình Google Drive: upload và tải tệp báo tạm thời không khả dụng");
            return new UnconfiguredStorageAdapter();
        }
        String dir = properties.localStorageDir() == null || properties.localStorageDir().isBlank()
                ? Path.of(System.getProperty("java.io.tmpdir"), "platform-files").toString()
                : properties.localStorageDir();
        log.warn("Chưa cấu hình Google Drive: lưu tệp vào thư mục local {}", dir);
        return new LocalFolderStorageAdapter(Path.of(dir));
    }
}
