package edu.aiplatform.attempts.port;

import java.time.Instant;
import java.util.UUID;

public record SubmissionView(UUID attemptId, UUID assignmentId, UUID accountId, int attemptNo, AttemptStatus status,
                             String contentJson, Instant submittedAt, boolean late, boolean gradedAttempt) {}
