package edu.aiplatform.assignmentwork.contract;

import java.time.Instant;
import java.util.UUID;

/**
 * Tiến độ bài nhóm của một sinh viên: {@code submitted} là nhóm đã có bản nộp; {@code changedSinceSubmit} là đã sửa sau lần
 * nộp cuối (bài quay về Đang làm, nộp lại được).
 */
public record GroupWorkStatus(UUID assignmentId, UUID groupDocumentId, boolean submitted, Instant submittedAt,
                              boolean changedSinceSubmit) {
}
