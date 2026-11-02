package edu.aiplatform.files.service;

import edu.aiplatform.files.contract.ArtifactPurpose;
import java.util.UUID;

/** Thuộc tính lưu cùng tệp (BR-U03-05). */
public record StoredMetadata(UUID ownerAccountId, ArtifactPurpose purpose, String originalFileName, String mediaType,
                             long byteSize, String sha256) {
}
