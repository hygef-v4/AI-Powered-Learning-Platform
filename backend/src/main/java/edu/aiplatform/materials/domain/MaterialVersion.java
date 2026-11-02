package edu.aiplatform.materials.domain;

import java.time.Instant;
import java.util.UUID;

public record MaterialVersion(UUID materialId, int versionNo, String fileId, String fileName, String mimeType,
                              Long byteSize, String videoUrl, String videoId, TextStatus textStatus,
                              String extractedText, UUID createdBy, Instant createdAt) {
    public enum TextStatus { PENDING, EXTRACTED, NO_TEXT, FAILED }
}
