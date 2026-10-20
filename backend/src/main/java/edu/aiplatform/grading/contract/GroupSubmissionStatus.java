package edu.aiplatform.grading.contract;

import java.time.Instant;
import java.util.UUID;

public record GroupSubmissionStatus(UUID groupDocumentId, UUID groupId, String groupName, boolean submitted,
        Instant submittedAt, boolean late) {
}
