package edu.aiplatform.files.infrastructure;

import edu.aiplatform.files.port.StoragePort;
import edu.aiplatform.files.port.StorageUnavailableException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * Production/demo (`U03_REQUIRE_DRIVE=true`) mà thiếu key Google Drive: backend vẫn khởi động, mọi thao tác tệp báo
 * tạm thời không khả dụng (NFR-U03-14), không lưu tệp vào container.
 */
@Component
@ConditionalOnExpression("'${platform.files.drive.service-account-key:}'.isBlank() and ${platform.files.require-drive:false}")
public class UnconfiguredStorageAdapter implements StoragePort {

    private static final Logger log = LoggerFactory.getLogger(UnconfiguredStorageAdapter.class);

    public UnconfiguredStorageAdapter() {
        log.error("GOOGLE_DRIVE_SERVICE_ACCOUNT_KEY is not set; file upload and download are unavailable");
    }

    @Override
    public String put(Path content, Map<String, String> metadata) {
        throw unavailable();
    }

    @Override
    public InputStream get(String fileId) {
        throw unavailable();
    }

    @Override
    public Map<String, String> readMetadata(String fileId) {
        throw unavailable();
    }

    @Override
    public void delete(String fileId) {
        throw unavailable();
    }

    private static StorageUnavailableException unavailable() {
        return new StorageUnavailableException("Google Drive is not configured", null);
    }
}
