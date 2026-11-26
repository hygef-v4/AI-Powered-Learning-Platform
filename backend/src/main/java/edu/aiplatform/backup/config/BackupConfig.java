package edu.aiplatform.backup.config;

import edu.aiplatform.backup.integration.DriveBackupStore;
import edu.aiplatform.backup.integration.UnconfiguredBackupStore;
import edu.aiplatform.backup.service.IBackupStore;
import edu.aiplatform.files.config.FilesProperties;
import edu.aiplatform.files.integration.DriveClientFactory;
import edu.aiplatform.files.integration.DriveFolders;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Backup chỉ lưu trên Google Drive: Shared Drive có {@code GOOGLE_BACKUP_FOLDER_ID} thì dùng thư mục đó, còn lại dùng
 * thư mục {@code backups} trong thư mục gốc của app. Không cấu hình Drive thì không lưu backup ở đâu cả.
 */
@Configuration
@EnableConfigurationProperties(BackupProperties.class)
public class BackupConfig {

    @Bean
    IBackupStore backupStore(BackupProperties backup, FilesProperties files, DriveClientFactory clients,
                             DriveFolders folders) {
        if (files.hasSharedDrive() && backup.folderId() != null && !backup.folderId().isBlank()) {
            return new DriveBackupStore(clients, folders, backup::folderId);
        }
        if (files.hasDrive()) {
            return new DriveBackupStore(clients, folders, () -> folders.ensureChild(folders.resolveRoot(), "backups"));
        }
        // Không có Drive thì không lưu backup ở đâu cả: lịch chạy bỏ qua, không tạo lần chạy (yêu cầu 2026-11-26).
        return new UnconfiguredBackupStore();
    }
}
