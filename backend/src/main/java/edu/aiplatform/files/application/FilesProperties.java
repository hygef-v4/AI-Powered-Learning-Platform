package edu.aiplatform.files.application;

import java.nio.file.Path;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Cấu hình tệp `platform.files.*` (logical-components §3). */
@ConfigurationProperties("platform.files")
public record FilesProperties(
        int maxConcurrentUploads,
        Path uploadTmpDir,
        Duration downloadTokenTtl,
        String fileRefSecret,
        Duration fileRefTtl,
        Path localStorageDir,
        boolean requireDrive,
        Drive drive) {

    public record Drive(String serviceAccountKey, String sharedDriveId) {}
}
