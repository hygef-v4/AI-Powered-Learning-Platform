package edu.aiplatform.assignmentwork.application;

import com.fasterxml.jackson.databind.JsonNode;
import edu.aiplatform.assignments.contract.AssignmentScope;
import edu.aiplatform.assignments.contract.AssignmentStatus;
import edu.aiplatform.assignments.contract.DiagramType;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.DocumentPart;
import edu.aiplatform.assignments.contract.GradingMode;
import edu.aiplatform.assignments.contract.SubmissionMode;
import edu.aiplatform.assignments.contract.SubmissionWindow;
import edu.aiplatform.assignments.contract.ValidationIssue;
import edu.aiplatform.assignmentwork.contract.AttemptStatus;
import edu.aiplatform.assignmentwork.contract.SubmitMode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Dữ liệu trả về API U08 (domain-entities mục 5). Tài liệu trả cho frontend không có {@code fileId} của ảnh. */
public final class WorkViews {

    private WorkViews() {
    }

    /** {@code myStatus}: NOT_STARTED, IN_PROGRESS, SUBMITTED, SUBMITTED_LATE, MISSED, GROUP. */
    public record Row(UUID assignmentId, UUID classId, String classCode, String className, AssignmentScope scope,
                      String title, SubmissionMode submissionMode, GradingMode gradingMode, boolean diagramEnabled,
                      AssignmentStatus status, Instant opensAt, Instant closesAt, Instant lateUntil,
                      Integer maxAttempts, String myStatus, int attemptCount, int submittedCount,
                      UUID inProgressAttemptId, Instant lastSubmittedAt, GroupWork group) {
    }

    public record GroupWork(UUID groupDocumentId, boolean submitted, Instant submittedAt) {
    }

    public record AttemptSummary(UUID attemptId, int attemptNo, AttemptStatus status, Instant startedAt,
                                 Instant deadlineAt, Instant lastSavedAt, Instant submittedAt, boolean late,
                                 SubmitMode submitMode, String receiptHash, boolean graded, long feedbackCount,
                                 List<ValidationIssue> warnings) {
    }

    /**
     * Trang thông tin trước khi làm bài (yêu cầu 2026-10-21 thay BR-U08-03): chỉ thông tin, không có hướng dẫn, khung
     * đề và rubric; đề hiện trong lượt làm, rubric không hiện cho người học.
     */
    public record Detail(UUID assignmentId, UUID classId, String classCode, String className, AssignmentScope scope,
                         String title, SubmissionMode submissionMode, GradingMode gradingMode, boolean diagramEnabled,
                         AssignmentStatus status, Instant opensAt, Instant closesAt, Instant lateUntil,
                         Integer maxAttempts, int partCount, BigDecimal maxPoints,
                         Map<DiagramType, Integer> requiredDiagrams, boolean activityMonitoringEnabled,
                         String activityNotice, List<UUID> referenceMaterialIds, int attemptsUsed,
                         AttemptSummary latestAttempt, UUID inProgressAttemptId, boolean canStart,
                         String startBlockedReason, SubmissionWindow window, Instant serverTime) {
    }

    /** Lượt làm: đề (hướng dẫn, khung trong {@code content}) và các phần; không có rubric (yêu cầu 2026-10-21). */
    public record Attempt(UUID attemptId, UUID assignmentId, UUID classId, String title, int attemptNo,
                          int maxAttempts, AttemptStatus status, DocumentModel content, long contentVersion,
                          Instant startedAt, Instant deadlineAt, Instant lastSavedAt, Instant submittedAt, boolean late,
                          SubmitMode submitMode, String receiptHash, List<ValidationIssue> warnings,
                          String instructions, GradingMode gradingMode, SubmissionMode submissionMode,
                          boolean diagramEnabled, Map<DiagramType, Integer> requiredDiagrams, List<DocumentPart> parts,
                          BigDecimal maxPoints, boolean activityMonitoringEnabled,
                          String activityNotice, Instant closesAt, Instant lateUntil, boolean editable,
                          Instant serverTime) {
    }

    public record Saved(UUID attemptId, long contentVersion, Instant lastSavedAt, DocumentModel content) {
    }

    public record Receipt(UUID attemptId, UUID assignmentId, UUID classId, String title, int attemptNo,
                          int maxAttempts, Instant submittedAt, boolean late, SubmitMode submitMode, String receiptHash,
                          int blockCount, int diagramCount) {
    }

    public record Feedback(UUID feedbackId, UUID attemptId, String origin, String status, UUID taskId,
                           JsonNode feedback, JsonNode provenance, String safeError, Instant createdAt,
                           Instant completedAt) {
    }

    public record Diagram(UUID attemptId, UUID blockId, String title, String diagramType, String alt, String xml,
                          String svgPreview) {
    }

    public record ImportPreview(DocumentModel document, List<ValidationIssue> warnings, int imageCount,
                                int diagramCount) {
    }
}
