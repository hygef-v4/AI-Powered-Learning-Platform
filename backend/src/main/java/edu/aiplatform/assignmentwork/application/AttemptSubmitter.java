package edu.aiplatform.assignmentwork.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.assignments.contract.AssignmentContent;
import edu.aiplatform.assignments.contract.AssignmentView;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.IAssignmentQuery;
import edu.aiplatform.assignments.contract.IDocumentModel;
import edu.aiplatform.assignments.contract.ValidationIssue;
import edu.aiplatform.assignmentwork.contract.ISubmissionListener;
import edu.aiplatform.assignmentwork.contract.SubmitMode;
import edu.aiplatform.assignmentwork.domain.AssignmentAttempt;
import edu.aiplatform.assignmentwork.domain.AttemptRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Nộp lượt (BR-U08-21, 23; P2): câu lệnh có điều kiện trạng thái; {@code ISubmissionListener} (U09) chạy trong cùng
 * transaction; audit {@code ASSIGNMENT_SUBMITTED}. Dùng chung cho nộp tay, tự nộp khi hết hạn và khi ngưng giao.
 */
@Component
class AttemptSubmitter {

    private final AttemptRepository attempts;
    private final IAssignmentQuery assignments;
    private final IDocumentModel documents;
    private final ObjectProvider<ISubmissionListener> listeners;
    private final WorkAudit audit;
    private final ObjectMapper json;

    AttemptSubmitter(AttemptRepository attempts, IAssignmentQuery assignments, IDocumentModel documents,
                     ObjectProvider<ISubmissionListener> listeners, WorkAudit audit, ObjectMapper json) {
        this.attempts = attempts;
        this.assignments = assignments;
        this.documents = documents;
        this.listeners = listeners;
        this.audit = audit;
        this.json = json;
    }

    /** Nộp lượt đã khóa dòng; trả {@code false} nếu lượt đã nộp trước đó. */
    boolean submit(AssignmentAttempt attempt, SubmitMode mode, UUID actorId, Instant now, Instant closesAt,
                   List<ValidationIssue> warnings) {
        Instant effective = mode == SubmitMode.MANUAL ? now
                : attempt.lastSavedAt() != null ? attempt.lastSavedAt() : attempt.startedAt();
        boolean late = closesAt != null && effective.isAfter(closesAt);
        String hash = receiptHash(attempt.content());
        if (attempts.submit(attempt.id(), mode, late, hash, now, warnings) != 1) {
            return false;
        }
        listeners.orderedStream().forEach(listener -> listener.onSubmitted(attempt.id()));
        audit.success(actorId, "ASSIGNMENT_SUBMITTED", WorkAudit.OBJECT_ATTEMPT, attempt.id(), WorkAudit.details(
                "assignmentId", attempt.assignmentId(), "classId", attempt.classId(), "accountId", attempt.accountId(),
                "attemptNo", attempt.attemptNo(), "submitMode", mode, "late", late, "warningCount", warnings.size()));
        return true;
    }

    /**
     * Tự nộp bản đã lưu gần nhất, kể cả rỗng, không kiểm điều kiện nộp; lỗi kiểm ghi thành cảnh báo cho giảng viên.
     * Hết hạn mà bài vừa được kéo dài hạn thì lượt nhận hạn mới, chưa nộp.
     */
    boolean autoSubmit(UUID attemptId, SubmitMode mode, Instant now) {
        AssignmentAttempt attempt = attempts.findForUpdate(attemptId).orElse(null);
        if (attempt == null || !attempt.inProgress()) {
            return false;
        }
        AssignmentView view = assignments.get(attempt.assignmentId()).orElse(null);
        Instant closesAt = view == null ? attempt.snapshot().closesAt() : view.closesAt();
        if (mode == SubmitMode.AUTO_DEADLINE && view != null) {
            Instant deadline = view.lateUntil() != null ? view.lateUntil() : view.closesAt();
            if (deadline != null && deadline.isAfter(attempt.deadlineAt())) {
                attempts.extendDeadline(attempt.id(), deadline);
                if (!now.isAfter(deadline.plus(AssignmentAttempt.GRACE))) {
                    return false;
                }
            }
        }
        List<ValidationIssue> warnings = assignments.getContent(attempt.assignmentId())
                .map(AssignmentContent::skeleton)
                .map(skeleton -> documents.validateForSubmit(skeleton, attempt.content(),
                        attempt.snapshot().requiredDiagrams()).errors())
                .orElse(List.of());
        return submit(attempt, mode, null, now, closesAt, warnings);
    }

    String receiptHash(DocumentModel content) {
        try {
            byte[] bytes = json.writeValueAsString(content).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("Không tính được mã băm bài nộp", e);
        }
    }
}
