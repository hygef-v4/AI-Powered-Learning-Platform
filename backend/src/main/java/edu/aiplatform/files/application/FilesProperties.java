package edu.aiplatform.files.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình tệp: khóa ký {@code FileRef}, Drive, thư mục local khi chạy không có Drive.
 *
 * @param requireDrive production đặt {@code true}: thiếu Drive thì không lưu tệp vào container
 */
@ConfigurationProperties(prefix = "platform.files")
public record FilesProperties(String filerefSecret, boolean requireDrive, String localStorageDir, String uploadTmpDir,
                              String driveServiceAccountKey, String sharedDriveId) {

    public boolean driveConfigured() {
        return driveServiceAccountKey != null && !driveServiceAccountKey.isBlank()
                && sharedDriveId != null && !sharedDriveId.isBlank();
    }
}
