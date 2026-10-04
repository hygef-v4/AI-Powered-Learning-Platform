package edu.aiplatform.grading.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** U15 cung cấp cho U11, U16: chỉ điểm PUBLISHED. */
public interface GradeQueryPort {
    Optional<PublishedGrade> publishedForAttempt(UUID attemptId);
    List<PublishedGrade> publishedForStudentInClass(UUID accountId, UUID classId);
    List<PublishedGrade> publishedForAssignment(UUID assignmentId);
}
