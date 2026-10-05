package edu.aiplatform.files.application;

import edu.aiplatform.files.port.FileRef;
import java.time.Instant;

/** Kết quả upload trả cho frontend (F1 bước 7); không có `fileId`. */
public record UploadResult(FileRef fileRef, Instant expiresAt, String mediaType, long byteSize, String originalFileName) {}
