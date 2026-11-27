package edu.aiplatform.backup.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình backup: thư mục Drive riêng, public key bọc khóa archive (tùy chọn), role {@code backup_replicator} cho
 * {@code pg_basebackup}, thư mục WAL archive dùng chung với Postgres và giới hạn tốc độ đọc khi chụp base backup.
 */
@ConfigurationProperties(prefix = "platform.backup")
public record BackupProperties(String folderId, String publicKey, String dbHost, int dbPort, String dbName,
                               String dbUser, String dbPassword, String tmpDir, String walDir, String maxRate) {
}
