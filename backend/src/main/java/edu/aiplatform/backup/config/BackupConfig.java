package edu.aiplatform.backup.config;

import edu.aiplatform.backup.integration.DriveBackupStore;
import edu.aiplatform.backup.integration.LocalBackupStore;
import edu.aiplatform.backup.integration.UnconfiguredBackupStore;

import edu.aiplatform.backup.config.BackupProperties;
import edu.aiplatform.backup.service.IBackupStore;
import edu.aiplatform.files.config.FilesProperties;
import edu.aiplatform.files.integration.DriveClientFactory;
import java.nio.file.Path;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Chọn thư mục backup: có Drive và {@code GOOGLE_BACKUP_FOLDER_ID} thì Drive; production bắt buộc Drive. */
@Configuration
@EnableConfigurationProperties(BackupProperties.class)
public class BackupConfig {

    @Bean
    IBackupStore backupStore(BackupProperties backup, FilesProperties files, DriveClientFactory clients) {
        if (files.driveConfigured() && backup.folderId() != null && !backup.folderId().isBlank()) {
            return new DriveBackupStore(clients, backup.folderId());
        }
        if (files.requireDrive()) {
            return new UnconfiguredBackupStore();
        }
        String dir = backup.localDir() == null || backup.localDir().isBlank()
                ? Path.of(System.getProperty("java.io.tmpdir"), "platform-backups").toString() : backup.localDir();
        return new LocalBackupStore(Path.of(dir));
    }
}
