package edu.aiplatform.files.domain;

import edu.aiplatform.files.port.ArtifactPurpose;
import java.time.Instant;
import java.util.UUID;

/** Nội dung có chữ ký của `FileRef`: tệp, người tải lên, mục đích, hạn 1 giờ (domain-entities §2). */
public record FileRefClaims(String fileId, UUID ownerAccountId, ArtifactPurpose purpose, Instant expiresAt) {}
