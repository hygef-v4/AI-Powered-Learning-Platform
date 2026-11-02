package edu.aiplatform.files.application;

import edu.aiplatform.files.contract.FileRef;
import java.time.Instant;

public record UploadResult(FileRef fileRef, String mediaType, long byteSize, String originalFileName,
                           Instant expiresAt) {
}
