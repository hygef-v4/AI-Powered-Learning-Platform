package edu.aiplatform.assignmentwork.domain;

import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.ValidationIssue;
import edu.aiplatform.assignmentwork.contract.AttemptStatus;
import edu.aiplatform.assignmentwork.contract.SubmitMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Một lượt làm bài cá nhân (domain-entities U08 mục 2). Nộp thì nội dung bất biến; {@code contentVersion} bắt đầu 0,
 * tăng mỗi lần lưu (P1). {@code classId} là lớp người học làm bài, để giảng viên lớp đó chấm.
 */
public record AssignmentAttempt(UUID id, UUID assignmentId, UUID classId, UUID accountId, int attemptNo,
                                AttemptSnapshot snapshot, DocumentModel content, long contentVersion,
                                AttemptStatus status, SubmitMode submitMode, boolean late, Instant startedAt,
                                Instant deadlineAt, Instant lastSavedAt, Instant submittedAt, String receiptHash,
                                List<ValidationIssue> submitWarnings) {

    /** Lần lưu cuối gửi trong 30 giây sau hạn vẫn nhận (BR-U08-15, 20). */
    public static final Duration GRACE = Duration.ofSeconds(30);

    public AssignmentAttempt {
        submitWarnings = submitWarnings == null ? List.of() : List.copyOf(submitWarnings);
    }

    public boolean inProgress() {
        return status == AttemptStatus.IN_PROGRESS;
    }

    public boolean acceptsWrites(Instant now) {
        return inProgress() && !now.isAfter(deadlineAt.plus(GRACE));
    }
}
