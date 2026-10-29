package edu.aiplatform.ai.domain;

import java.time.Instant;
import java.util.UUID;

/** Credential MCP của một agent (domain-entities U06 mục 5); chỉ lưu SHA-256 của token (BR-U06-31). */
public record McpCredential(UUID id, UUID accountId, String label, Access access, String tokenHash, String tokenHint,
                            Instant expiresAt, Instant revokedAt, Instant lastUsedAt, Instant createdAt) {

    public enum Access {
        READ_ONLY, READ_WRITE
    }

    public enum Status {
        ACTIVE, EXPIRED, REVOKED
    }

    public Status status(Instant now) {
        if (revokedAt != null) {
            return Status.REVOKED;
        }
        return now.isBefore(expiresAt) ? Status.ACTIVE : Status.EXPIRED;
    }
}
