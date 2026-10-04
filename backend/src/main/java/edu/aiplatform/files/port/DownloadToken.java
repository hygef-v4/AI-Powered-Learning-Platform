package edu.aiplatform.files.port;

import java.time.Instant;

public record DownloadToken(String token, String url, Instant expiresAt) {}
