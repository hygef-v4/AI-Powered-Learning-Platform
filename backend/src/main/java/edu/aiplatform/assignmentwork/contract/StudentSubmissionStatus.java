package edu.aiplatform.assignmentwork.contract;

import java.time.Instant;
import java.util.UUID;

/** Trạng thái nộp của một sinh viên; {@code gradedAttemptId} là lượt được chấm. */
public record StudentSubmissionStatus(UUID accountId, UUID gradedAttemptId, int attemptCount,
                                      AttemptStatus latestStatus, Instant submittedAt, boolean late) {
}
