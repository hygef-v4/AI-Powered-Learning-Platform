package edu.aiplatform.grading.contract;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * U09 khai báo, U12 cài: hiện và chấm bài nhóm, tiến độ theo nhóm. Chưa có U12
 * thì không có đánh giá nhóm.
 */
public interface IGroupSubmissionSource {

    /** Finds the submitted content and membership for a group document. */
    Optional<GroupSubmissionView> getGroupSubmission(UUID groupDocumentId);

    /** Lists submission status for groups in an assignment and class. */
    List<GroupSubmissionStatus> listGroupSubmissions(UUID assignmentId, UUID classId);
}
