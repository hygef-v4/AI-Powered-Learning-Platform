package edu.aiplatform.audit.contract;

import java.time.Instant;
import java.util.UUID;

public record AuditFilter(UUID actorId, String action, String objectType, String objectId,
                          AuditResult result, Instant from, Instant to) {
}
