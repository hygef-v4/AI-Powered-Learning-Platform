package edu.aiplatform.grading.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.ai.contract.AiTaskFailure;
import edu.aiplatform.ai.contract.AiTaskState;
import edu.aiplatform.ai.contract.AiTaskView;
import edu.aiplatform.ai.contract.ApplyOutcome;
import edu.aiplatform.grading.model.GradingProposal;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Package-private helper for updating proposal dispatch, failure, and rejection state. */
final class GradingProposalCallbackSupport {
    private GradingProposalCallbackSupport() {
    }

    static UUID parseEvaluationTarget(String targetRef, String taskType) {
        if (!"GRADING_PROPOSAL".equals(taskType)
                || targetRef == null || !targetRef.startsWith("evaluation:")) {
            return null;
        }
        try {
            return UUID.fromString(targetRef.substring(11));
        } catch (RuntimeException ex) {
            return null;
        }
    }

    static void applyDispatchState(GradingProposal proposal, AiTaskView task, String error,
            ObjectMapper objectMapper, Clock clock) {
        if (task == null) {
            if ("PENDING".equals(proposal.status)) {
                proposal.status = "FAILED";
                proposal.safeError = error == null ? "AI service unavailable" : error;
                proposal.taskState = null;
                proposal.completedAt = clock.instant();
            }
            return;
        }
        if (proposal.taskId != null && !proposal.taskId.equals(task.taskId())) {
            proposal.status = "REJECTED";
            proposal.safeError = "Task correlation mismatch";
            proposal.taskState = null;
            if (proposal.completedAt == null) {
                proposal.completedAt = clock.instant();
            }
            return;
        }
        attachTaskMetadata(proposal, task, objectMapper);
        transitionProposalFromTaskView(proposal, task, clock);
    }

    private static void attachTaskMetadata(GradingProposal proposal, AiTaskView task,
            ObjectMapper objectMapper) {
        proposal.taskId = task.taskId();
        if (task.model() != null) {
            proposal.model = task.model();
        }
        if (proposal.usage == null || proposal.usage.isNull()
                || task.state() == AiTaskState.READY) {
            proposal.usage = objectMapper.valueToTree(task.usage());
        }
    }

    private static void transitionProposalFromTaskView(GradingProposal proposal, AiTaskView task,
            Clock clock) {
        if (("PENDING".equals(proposal.status) || isLegacyFailure(proposal))
                && Set.of(AiTaskState.FAILED, AiTaskState.CANCELLED, AiTaskState.UNKNOWN)
                        .contains(task.state())) {
            proposal.status = "FAILED";
            proposal.safeError = formatTaskError(task.safeError());
            proposal.taskState = null;
            if (proposal.completedAt == null) {
                proposal.completedAt = clock.instant();
            }
        } else if ("PENDING".equals(proposal.status) && task.state() == AiTaskState.READY
                && task.applyResult() != null && task.applyResult() != ApplyOutcome.APPLIED) {
            proposal.status = task.applyResult() == ApplyOutcome.STALE ? "STALE" : "REJECTED";
            if ("REJECTED".equals(proposal.status)) {
                proposal.safeError = task.safeError() == null || task.safeError().isBlank()
                        ? "AI proposal cannot be applied to this submission"
                        : formatTaskError(task.safeError());
            }
            proposal.taskState = null;
            if (proposal.completedAt == null) {
                proposal.completedAt = clock.instant();
            }
        } else if ("PENDING".equals(proposal.status)) {
            proposal.taskState = task.state().name();
        } else {
            proposal.taskState = null;
        }
    }

    static boolean matchesFailureTarget(GradingProposal proposal, AiTaskFailure failure) {
        boolean matchesTask = Objects.equals(proposal.taskId, failure.taskId())
                || (proposal.taskId == null && failure.inputVersion() != null
                        && proposal.versionNo == failure.inputVersion());
        boolean matchesActor = failure.actor() == null
                || proposal.requesterId.equals(failure.actor().accountId());
        return "PENDING".equals(proposal.status) && matchesTask && matchesActor;
    }

    static void applyFailure(GradingProposal proposal, AiTaskFailure failure, Clock clock) {
        proposal.taskId = failure.taskId();
        proposal.status = failure.state() == AiTaskState.READY ? "REJECTED" : "FAILED";
        proposal.taskState = null;
        proposal.safeError = formatTaskError(failure.safeError());
        proposal.completedAt = clock.instant();
    }

    static void applyRejectionFromException(GradingProposal proposal, UUID taskId,
            ApiException ex, Clock clock) {
        proposal.status = ex.status().value() == 409 ? "STALE" : "REJECTED";
        proposal.taskId = taskId;
        proposal.taskState = null;
        proposal.safeError = "INVALID_AI_PROPOSAL".equals(ex.code())
                ? "Phản hồi AI chưa hợp lệ ("
                        + GradingProposalValidator.buildRejectionDiagnostic(ex)
                        + "). Bạn vẫn có thể chấm thủ công."
                : "AI proposal cannot be applied to this submission";
        proposal.completedAt = clock.instant();
    }

    static boolean isLegacyFailure(GradingProposal proposal) {
        return "FAILED".equals(proposal.status) && (proposal.safeError == null
                || Set.of("AI task failed", "AI task unavailable or failed")
                        .contains(proposal.safeError));
    }

    static String formatTaskError(String diagnostic) {
        return diagnostic == null || diagnostic.isBlank()
                ? "Không thể tạo đề xuất AI. Bạn vẫn có thể chấm thủ công."
                : diagnostic;
    }
}
