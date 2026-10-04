package edu.aiplatform.attempts.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** U11 cung cấp cho U13, U15, U16. */
public interface SubmissionQueryPort {
    Optional<SubmissionView> get(UUID attemptId);
    List<SubmissionView> listByAssignment(UUID assignmentId);
    /** Lượt được chấm chính thức của bài GRADED là lượt nộp cuối (BR-U11-31). */
    Optional<SubmissionView> gradedAttempt(UUID assignmentId, UUID accountId);
}
