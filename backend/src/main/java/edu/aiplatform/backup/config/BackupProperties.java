package edu.aiplatform.backup.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Cấu hình backup: thư mục Drive riêng, public key bọc khóa archive, role chỉ đọc cho {@code pg_dump}. */
@ConfigurationProperties(prefix = "platform.backup")
public record BackupProperties(String folderId, String publicKey, String dbHost, int dbPort, String dbName,
                               String dbUser, String dbPassword, String tmpDir, String localDir) {
}
