package edu.aiplatform.assignments.web;

import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.ai.contract.AiSource;
import edu.aiplatform.assignments.application.AssignmentCommands;
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
        AssignmentCommands.Create command() {
            return new AssignmentCommands.Create(submissionMode, gradingMode, Boolean.TRUE.equals(diagramEnabled), title);
        }
    }

    record Copy(UUID sourceAssignmentId) {
    }

    record Details(Long version, String title, String instructions, Boolean diagramEnabled, GradingMode gradingMode,
                   Map<DiagramType, Integer> requiredDiagrams, List<UUID> referenceMaterialIds,
                   Boolean activityMonitoringEnabled, String activityNotice) {
        AssignmentCommands.Details command() {
            return new AssignmentCommands.Details(requireVersion(version), title, instructions, diagramEnabled, gradingMode,
                    requiredDiagrams, referenceMaterialIds, activityMonitoringEnabled, activityNotice);
        }
    }

    record Skeleton(Long version, DocumentModel skeleton, String instructions, Boolean fromDocx) {
        AssignmentCommands.Skeleton command() {
            return new AssignmentCommands.Skeleton(requireVersion(version), skeleton, instructions,
                    Boolean.TRUE.equals(fromDocx));
        }
    }

    record Version(Long version) {
        long value() {
            return requireVersion(version);
        }
    }

    record Publish(Long version, Instant opensAt, Boolean openNow, Instant closesAt, Instant lateUntil,
                   Integer maxAttempts, Boolean confirmUngroupedStudents) {
        AssignmentCommands.Publish command() {
            return new AssignmentCommands.Publish(requireVersion(version), opensAt, Boolean.TRUE.equals(openNow), closesAt,
                    lateUntil, maxAttempts, Boolean.TRUE.equals(confirmUngroupedStudents));
        }
    }

    record Schedule(Long version, Instant opensAt, Instant closesAt, Instant lateUntil, Integer maxAttempts) {
        AssignmentCommands.Schedule command() {
            return new AssignmentCommands.Schedule(requireVersion(version), opensAt, closesAt, lateUntil, maxAttempts);
        }
    }

    record Retire(Long version, String reason) {
        AssignmentCommands.Retire command() {
            return new AssignmentCommands.Retire(requireVersion(version), reason);
        }
    }

    record Rubric(Long version, List<RubricCriterion> criteria) {
        AssignmentCommands.RubricUpdate command() {
            return new AssignmentCommands.RubricUpdate(requireVersion(version), criteria);
        }
    }

    record AiDraft(String request, List<UUID> materialIds, AiSource source, AiProvider provider, String requestRef) {
        AssignmentCommands.AiDraftRequest command() {
            return new AssignmentCommands.AiDraftRequest(request, materialIds, source, provider, requestRef);
        }
    }

    record Apply(Long version, Boolean replaceInstructions) {
        AssignmentCommands.ApplyDraft command() {
            return new AssignmentCommands.ApplyDraft(requireVersion(version), Boolean.TRUE.equals(replaceInstructions));
        }
    }

    static long requireVersion(Long version) {
        if (version == null || version < 0) {
            throw ApiException.badRequest("VERSION_REQUIRED", "Thiếu phiên bản của bài");
        }
        return version;
    }
}
