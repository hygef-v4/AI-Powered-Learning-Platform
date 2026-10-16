package edu.aiplatform.assignmentwork.contract;

import edu.aiplatform.assignments.contract.DocumentModel;
import java.time.Instant;
import java.util.UUID;

public record SubmissionView(UUID attemptId, UUID assignmentId, UUID classId, UUID accountId, DocumentModel content,
                             String receiptHash, Instant submittedAt, boolean late, SubmitMode submitMode) {
}
