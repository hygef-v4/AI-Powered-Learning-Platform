package edu.aiplatform.files.config;

import edu.aiplatform.files.integration.DriveClientFactory;
import edu.aiplatform.files.integration.GoogleDriveStorageAdapter;
import edu.aiplatform.files.integration.LocalFolderStorageAdapter;
import edu.aiplatform.files.integration.UnconfiguredStorageAdapter;

import edu.aiplatform.files.config.FilesProperties;
import edu.aiplatform.files.service.IStorage;
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
    IStorage storage(FilesProperties properties, DriveClientFactory driveClients) {
        if (properties.driveConfigured()) {
            return new GoogleDriveStorageAdapter(driveClients, properties.sharedDriveId());
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
