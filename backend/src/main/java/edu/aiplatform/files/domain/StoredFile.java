package edu.aiplatform.files.domain;

import edu.aiplatform.files.port.ArtifactPurpose;
import edu.aiplatform.files.port.StoragePort;
import edu.aiplatform.files.port.StoredFileInfo;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tệp đã lưu: byte trên kho, metadata đi kèm tệp (`appProperties` trên Drive), không có bảng (domain-entities §2).
 * `fileId` không bao giờ trả cho frontend (BR-U03-22).
 */
public record StoredFile(String fileId, UUID ownerAccountId, ArtifactPurpose purpose, String mediaType,
                         String originalFileName, String sha256, long byteSize) {

    /** Metadata ghi cùng tệp khi upload (Bước 13). */
    public static Map<String, String> metadata(UUID ownerAccountId, ArtifactPurpose purpose, String mediaType,
                                               String originalFileName, String sha256) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put(StoragePort.OWNER_ACCOUNT_ID, ownerAccountId.toString());
        m.put(StoragePort.PURPOSE, purpose.name());
        m.put(StoragePort.MEDIA_TYPE, mediaType);
        m.put(StoragePort.ORIGINAL_FILE_NAME, originalFileName);
        m.put(StoragePort.SHA256, sha256);
        return m;
    }

    public static StoredFile fromMetadata(String fileId, Map<String, String> m) {
        return new StoredFile(fileId,
                UUID.fromString(m.get(StoragePort.OWNER_ACCOUNT_ID)),
                ArtifactPurpose.valueOf(m.get(StoragePort.PURPOSE)),
                m.get(StoragePort.MEDIA_TYPE),
                m.get(StoragePort.ORIGINAL_FILE_NAME),
                m.get(StoragePort.SHA256),
                Long.parseLong(m.getOrDefault(StoragePort.BYTE_SIZE, "0")));
    }

    public StoredFileInfo info() {
        return new StoredFileInfo(fileId, mediaType, byteSize, originalFileName);
    }
}
