package edu.aiplatform.files.infrastructure;

import edu.aiplatform.files.port.*;
import java.io.InputStream;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * Khung cài đặt của U03: thân hàm tạm trả giá trị rỗng/an toàn hoặc báo chưa cài.
 * Plan U03: Bước 15; dùng khi có key.
 */
@Component
@ConditionalOnExpression("'${GOOGLE_DRIVE_SERVICE_ACCOUNT_KEY:}' != ''")
public class GoogleDriveStorageAdapter implements StoragePort {

    @Override
    public String put(InputStream content, Map<String, String> metadata) {
        throw new UnsupportedOperationException("Chưa cài: U03");
    }

    @Override
    public InputStream get(String fileId) {
        throw new UnsupportedOperationException("Chưa cài: U03");
    }

    @Override
    public Map<String, String> readMetadata(String fileId) {
        return Map.of();
    }

    @Override
    public void delete(String fileId) {
        // Chưa cài: U03.
    }
}
