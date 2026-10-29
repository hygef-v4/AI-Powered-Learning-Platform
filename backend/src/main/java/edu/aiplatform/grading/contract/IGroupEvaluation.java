package edu.aiplatform.grading.contract;

import java.util.List;
import java.util.UUID;

/**
 * U12 gọi trong transaction nộp tài liệu nhóm: tạo hoặc đưa về chờ chấm đánh
 * giá nhóm và thành viên.
 */
public interface IGroupEvaluation {

    void onGroupSubmitted(UUID groupDocumentId, String submissionHash, UUID assignmentId, UUID classId,
            List<UUID> memberIds);
}
