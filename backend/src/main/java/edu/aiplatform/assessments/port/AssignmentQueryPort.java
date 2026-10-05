package edu.aiplatform.assessments.port;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** U08 cung cấp cho U09-U11, U14-U16. */
public interface AssignmentQueryPort {
    Optional<AssignmentView> get(UUID assignmentId);
    List<AssignmentView> listForClass(UUID classId);
    SubmissionWindow isSubmissionOpen(UUID assignmentId, Instant now);
}
