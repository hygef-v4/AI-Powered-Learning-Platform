package edu.aiplatform.assignments.application;

import com.fasterxml.jackson.databind.JsonNode;
import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.assignments.contract.AssignmentScope;
import edu.aiplatform.assignments.contract.AssignmentStatus;
import edu.aiplatform.assignments.contract.DiagramType;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.GradingMode;
import edu.aiplatform.assignments.contract.RubricCriterion;
import edu.aiplatform.assignments.contract.SubmissionMode;
import edu.aiplatform.assignments.contract.ValidationIssue;
import edu.aiplatform.assignments.domain.AssignmentOrigin;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Dữ liệu trả về API U07 (domain-entities mục 6). Khung trả cho frontend không có {@code fileId} của ảnh. */
public final class AssignmentViews {

    private AssignmentViews() {
    }

    public record Row(UUID id, AssignmentScope scope, UUID classId, UUID subjectId, String title,
                      SubmissionMode submissionMode, GradingMode gradingMode, boolean diagramEnabled,
                      AssignmentStatus status, int versionNo, AssignmentOrigin origin, Instant opensAt,
                      Instant closesAt, Instant lateUntil, Integer maxAttempts, Instant updatedAt, boolean readOnly) {
    }

    public record Page(List<Row> items, int hiddenCount, Map<AssignmentStatus, Long> counts, boolean groupSupported,
                       boolean ownerArchived) {
    }

    public record Part(String partId, UUID headingBlockId, List<UUID> ancestorHeadingIds, String title, UUID rubricId,
                       int criteriaCount, BigDecimal totalPoints, boolean rubricComplete, boolean locked) {
    }

    public record Permissions(boolean canManage, boolean canEditContent, boolean canChangeSchedule,
                              boolean canRetire, boolean canDelete, boolean canReview, boolean canPublish,
                              boolean canClone, boolean canCreateVersion, boolean ownerArchived) {
    }

    public record Detail(UUID id, AssignmentScope scope, UUID classId, UUID subjectId, String ownerName,
                         UUID sourceAssignmentId, AssignmentOrigin origin, int versionNo,
                         SubmissionMode submissionMode, boolean diagramEnabled, GradingMode gradingMode, String title,
                         String instructions, DocumentModel skeleton, List<Part> parts, BigDecimal totalPoints,
                         Map<DiagramType, Integer> requiredDiagrams, List<UUID> referenceMaterialIds,
                         boolean activityMonitoringEnabled, String activityNotice, boolean activityRetentionConfigured,
                         AssignmentStatus status, Instant opensAt, Instant closesAt, Instant lateUntil,
                         Integer maxAttempts, String retiredReason, Instant createdAt, Instant updatedAt, long version,
                         Permissions permissions, boolean groupSupported, List<ValidationIssue> warnings) {
    }

    public record Rubric(UUID rubricId, UUID assignmentId, String partId, String partTitle,
                         List<RubricCriterion> criteria, BigDecimal totalPoints, boolean locked, boolean complete,
                         boolean diagramEnabled, boolean editable, long assignmentVersion) {
    }

    public record CopySource(UUID classId, String code, String name, ClassStatus status, List<Row> assignments) {
    }

    public record Diagram(UUID assignmentId, UUID blockId, String assignmentTitle, String diagramType, String alt,
                          String xml, String svgPreview) {
    }

    public record AiDraft(UUID draftId, UUID assignmentId, String origin, String status, UUID taskId,
                          JsonNode proposal, JsonNode provenance, String safeError, long baseVersion,
                          Instant createdAt, Instant decidedAt) {
    }

    public record ImportPreview(DocumentModel document, List<ValidationIssue> warnings, int headingCount,
                                int imageCount, int diagramCount) {
    }

    public record Material(UUID materialId, String title, boolean hasText) {
    }
}
