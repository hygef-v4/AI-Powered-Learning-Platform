package edu.aiplatform.assignmentwork.contract;

import java.time.Instant;
import java.util.UUID;

public record GroupWorkStatus(UUID assignmentId, UUID groupDocumentId, boolean submitted, Instant submittedAt) {
}
