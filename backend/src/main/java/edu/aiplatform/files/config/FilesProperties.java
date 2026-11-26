package edu.aiplatform.files.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình tệp: khóa ký {@code FileRef}, Drive, thư mục local khi chạy không có Drive. Drive có hai cách: Shared Drive
 * với service account (Google Workspace), hoặc Drive cá nhân (Gmail) với OAuth refresh token của chính chủ tài khoản.
 *
 * @param driveOauthRefreshToken refresh token cấp cho scope {@code drive.file} của tài khoản Gmail chứa tệp
 * @param driveFolderName tên thư mục gốc app tự tạo trong My Drive (Drive cá nhân); bỏ trống dùng
 *     {@value #DEFAULT_FOLDER_NAME}
 * @param driveFolderId id thư mục gốc dùng thẳng, phải là thư mục do app tạo (scope {@code drive.file} không thấy
 *     thư mục tạo tay); không truy cập được thì quay về tìm hoặc tạo theo tên
 */
@ConfigurationProperties(prefix = "platform.files")
public record FilesProperties(String filerefSecret, String localStorageDir, String uploadTmpDir,
                              String driveServiceAccountKey, String sharedDriveId, String driveOauthClientId,
                              String driveOauthClientSecret, String driveOauthRefreshToken, String driveFolderName,
                              String driveFolderId) {

    public static final String DEFAULT_FOLDER_NAME = "AI Learning Platform";

    /** Tên thư mục gốc trên My Drive; cấu hình rỗng thì dùng tên mặc định. */
    public String resolveRootFolderName() {
        return isPresent(driveFolderName) ? driveFolderName.strip() : DEFAULT_FOLDER_NAME;
    }

    /** Shared Drive với service account; có cả hai cách thì dùng cách này. */
    public boolean hasSharedDrive() {
        return isPresent(driveServiceAccountKey) && isPresent(sharedDriveId);
    }

    /** Drive cá nhân: tệp nằm trong thư mục app tự tạo ở My Drive của tài khoản đã cấp refresh token. */
    public boolean hasPersonalDrive() {
        return !hasSharedDrive() && isPresent(driveOauthClientId) && isPresent(driveOauthClientSecret)
                && isPresent(driveOauthRefreshToken);
    }

    public boolean hasDrive() {
        return hasSharedDrive() || hasPersonalDrive();
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
