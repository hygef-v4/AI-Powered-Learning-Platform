package edu.aiplatform.assignments.controller;

import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.ai.contract.AiSource;
import edu.aiplatform.assignments.service.AssignmentCommands;
import edu.aiplatform.assignments.contract.DiagramType;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.GradingMode;
import edu.aiplatform.assignments.contract.RubricCriterion;
import edu.aiplatform.assignments.contract.SubmissionMode;
import edu.aiplatform.shared.error.ApiException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Body của API U07; chuyển thành lệnh của tầng application, thiếu {@code version} thì 400. */
final class AssignmentRequests {

    private AssignmentRequests() {
    }

    record Create(SubmissionMode submissionMode, GradingMode gradingMode, Boolean diagramEnabled, String title) {
        AssignmentCommands.Create buildCommand() {
            return new AssignmentCommands.Create(submissionMode, gradingMode, Boolean.TRUE.equals(diagramEnabled), title);
        }
    }

    /** {@code title} bỏ trống thì giữ tên bài nguồn. */
    /** {@code submissionMode}, {@code gradingMode}: loại của bài copy; bỏ trống thì giữ loại của bài nguồn. */
    record Copy(UUID sourceAssignmentId, String title, SubmissionMode submissionMode, GradingMode gradingMode) {
    }

    record Details(Long version, String title, String instructions, Boolean diagramEnabled, GradingMode gradingMode,
                   Map<DiagramType, Integer> requiredDiagrams, List<UUID> referenceMaterialIds,
                   Boolean activityMonitoringEnabled, String activityNotice, SubmissionMode submissionMode) {
        AssignmentCommands.Details buildCommand() {
            return new AssignmentCommands.Details(requireVersion(version), title, instructions, diagramEnabled, gradingMode,
                    requiredDiagrams, referenceMaterialIds, activityMonitoringEnabled, activityNotice, submissionMode);
        }
    }

    record Skeleton(Long version, DocumentModel skeleton, String instructions, Boolean fromDocx) {
        AssignmentCommands.Skeleton buildCommand() {
            return new AssignmentCommands.Skeleton(requireVersion(version), skeleton, instructions,
                    Boolean.TRUE.equals(fromDocx));
        }
    }

    record Version(Long version) {
        long readVersion() {
            return requireVersion(version);
        }
    }

    record Publish(Long version, Instant opensAt, Boolean openNow, Instant closesAt, Instant lateUntil,
                   Integer maxAttempts, Boolean confirmUngroupedStudents) {
        AssignmentCommands.Publish buildCommand() {
            return new AssignmentCommands.Publish(requireVersion(version), opensAt, Boolean.TRUE.equals(openNow), closesAt,
                    lateUntil, maxAttempts, Boolean.TRUE.equals(confirmUngroupedStudents));
        }
    }

    record Schedule(Long version, Instant opensAt, Instant closesAt, Instant lateUntil, Integer maxAttempts) {
        AssignmentCommands.Schedule buildCommand() {
            return new AssignmentCommands.Schedule(requireVersion(version), opensAt, closesAt, lateUntil, maxAttempts);
        }
    }

    record Retire(Long version, String reason) {
        AssignmentCommands.Retire buildCommand() {
            return new AssignmentCommands.Retire(requireVersion(version), reason);
        }
    }

    record Close(Long version) {
        long readVersion() {
            return requireVersion(version);
        }
    }

    record Rubric(Long version, List<RubricCriterion> criteria) {
        AssignmentCommands.RubricUpdate buildCommand() {
            return new AssignmentCommands.RubricUpdate(requireVersion(version), criteria);
        }
    }

    record AiDraft(String request, List<UUID> materialIds, List<String> sourceFileRefs,
                   AssignmentCommands.DraftOptions options, AiSource source, AiProvider provider, String requestRef,
                   String formatText, UUID templateAssignmentId, String mode, Boolean preserveTemplateQuestionPoints) {
        AssignmentCommands.AiDraftRequest buildCommand() {
            return new AssignmentCommands.AiDraftRequest(request, materialIds, sourceFileRefs, source, provider,
                    requestRef, options, formatText, templateAssignmentId, mode,
                    Boolean.TRUE.equals(preserveTemplateQuestionPoints));
        }
    }

    record AiSources(List<String> sourceFileRefs) {
    }

    /** {@code placement}: chỉ dùng cho bản nháp nhập đề, {@code REPLACE} (mặc định) hoặc {@code APPEND} (BR-U07-98). */
    record Apply(Long version, Boolean replaceInstructions, Boolean replaceTitle, String placement) {
        AssignmentCommands.ApplyDraft buildCommand() {
            return new AssignmentCommands.ApplyDraft(requireVersion(version), Boolean.TRUE.equals(replaceInstructions),
                    Boolean.TRUE.equals(replaceTitle), placement);
        }
    }

    static long requireVersion(Long version) {
        if (version == null || version < 0) {
            throw ApiException.createBadRequest("VERSION_REQUIRED", "Thiếu phiên bản của bài");
        }
        return version;
    }
}
