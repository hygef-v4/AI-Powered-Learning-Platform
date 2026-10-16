package edu.aiplatform.files.contract;

import java.time.Instant;

/** Link tải 5 phút, chỉ người được cấp dùng được. */
public record DownloadToken(String url, Instant expiresAt) {
}
