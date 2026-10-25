package edu.aiplatform.backup.infrastructure;

import edu.aiplatform.backup.application.BackupProperties;
import edu.aiplatform.backup.application.IBackupStore;
import edu.aiplatform.files.application.FilesProperties;
import edu.aiplatform.files.infrastructure.DriveClientFactory;
import edu.aiplatform.files.infrastructure.DriveFolders;
import java.nio.file.Path;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Chọn thư mục backup: Shared Drive cần {@code GOOGLE_BACKUP_FOLDER_ID}; Drive cá nhân dùng thư mục {@code backups}
 * trong thư mục app; production bắt buộc Drive.
 */
@Configuration
@EnableConfigurationProperties(BackupProperties.class)
public class BackupConfig {

    @Bean
    IBackupStore backupStore(BackupProperties backup, FilesProperties files, DriveClientFactory clients,
                             DriveFolders folders) {
        if (files.sharedDriveConfigured() && backup.folderId() != null && !backup.folderId().isBlank()) {
            return new DriveBackupStore(clients, folders, backup::folderId);
        }
        if (files.personalDriveConfigured()) {
            return new DriveBackupStore(clients, folders, () -> folders.child(folders.root(), "backups"));
        }
        if (files.requireDrive()) {
            return new UnconfiguredBackupStore();
        }
        String dir = backup.localDir() == null || backup.localDir().isBlank()
                ? Path.of(System.getProperty("java.io.tmpdir"), "platform-backups").toString() : backup.localDir();
        return new LocalBackupStore(Path.of(dir));
    }
}
