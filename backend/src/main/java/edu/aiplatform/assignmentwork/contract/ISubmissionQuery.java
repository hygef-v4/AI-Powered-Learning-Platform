package edu.aiplatform.assignmentwork.contract;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Chấm và theo dõi nộp bài (U09); nhắc hạn người chưa nộp (U14). */
public interface ISubmissionQuery {

    List<StudentSubmissionStatus> listSubmissions(UUID assignmentId, UUID classId);

    Optional<SubmissionView> getSubmission(UUID attemptId);

    List<UUID> listStudentsWithoutSubmission(UUID assignmentId, UUID classId);
}
