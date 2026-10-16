package edu.aiplatform.files.contract;

import java.util.UUID;

/** {@code fileId} chỉ lưu ở bảng của unit sở hữu, không trả cho frontend. */
public record StoredFileInfo(String fileId, UUID ownerAccountId, ArtifactPurpose purpose, String originalFileName,
                             String mediaType, long byteSize, String sha256) {
}
