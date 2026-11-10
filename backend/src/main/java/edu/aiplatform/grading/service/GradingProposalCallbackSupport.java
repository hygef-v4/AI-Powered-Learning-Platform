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
                proposal.safeError = formatTaskError(error);
                proposal.taskState = null;
                proposal.completedAt = clock.instant();
            }
            return;
        }
        if (proposal.taskId != null && !proposal.taskId.equals(task.taskId())) {
            proposal.status = "REJECTED";
            proposal.safeError = formatTaskError("Task correlation mismatch");
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
                        ? formatTaskError("AI proposal cannot be applied to this submission")
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
                ? formatTaskError(GradingProposalValidator.buildRejectionDiagnostic(ex))
                : formatTaskError(ex.code());
        proposal.completedAt = clock.instant();
    }

    static boolean isLegacyFailure(GradingProposal proposal) {
        return "FAILED".equals(proposal.status) && (proposal.safeError == null
                || Set.of("AI task failed", "AI task unavailable or failed")
                        .contains(proposal.safeError));
    }

    static String formatTaskError(String diagnostic) {
        String fallback = "Không thể tạo đề xuất AI. Hãy thử lại hoặc chấm thủ công.";
        if (diagnostic == null || diagnostic.isBlank()) return fallback;
        var legacy = java.util.regex.Pattern.compile("(?:PROPOSAL|FEEDBACK)_[A-Z_]+").matcher(diagnostic);
        boolean coded = legacy.find();
        String code = coded ? legacy.group() : diagnostic;
        return switch (code) {
            case "PROPOSAL_TOTAL_MISMATCH" -> "Tổng điểm AI trả về không khớp với tổng điểm từng tiêu chí rubric. Hãy thử chấm lại bằng AI hoặc chấm thủ công.";
            case "PROPOSAL_CHECKS_MISMATCH" -> "Các mục AI đánh dấu đạt không khớp với điểm chi tiết. Hãy thử chấm lại bằng AI hoặc chấm thủ công.";
            case "PROPOSAL_SECTION_MISMATCH" -> "AI nhận xét nhầm phần của bài làm. Hãy thử chấm lại bằng AI.";
            case "PROPOSAL_UNKNOWN_ITEM", "FEEDBACK_UNKNOWN_ITEM" -> "AI trả điểm hoặc nhận xét cho tiêu chí không có trong rubric. Hãy thử chấm lại bằng AI.";
            case "FEEDBACK_ITEM_COVERAGE" -> "AI chưa nhận xét đầy đủ các tiêu chí rubric. Hãy thử chấm lại bằng AI.";
            case "FEEDBACK_DUPLICATE_ITEM" -> "AI trả nhận xét trùng cho cùng một tiêu chí. Hãy thử chấm lại bằng AI.";
            case "FEEDBACK_UNKNOWN_EVIDENCE_BLOCK" -> "AI dẫn minh chứng không có trong bài nộp. Hãy kiểm tra minh chứng hoặc chấm lại bằng AI.";
            case "FEEDBACK_MISSING_EVIDENCE" -> "AI chưa cung cấp minh chứng để giải thích điểm. Hãy thử chấm lại bằng AI.";
            case "FEEDBACK_MISSING_REVIEW_REASON" -> "AI yêu cầu giảng viên kiểm tra nhưng chưa giải thích lý do. Hãy thử chấm lại bằng AI.";
            case "FEEDBACK_UNKNOWN_FIELD", "PROPOSAL_SCHEMA_MISMATCH" -> "Dữ liệu chấm điểm AI trả về không đúng cấu trúc yêu cầu. Hãy thử lại hoặc chấm thủ công.";
            case "AI_QUOTA_EXCEEDED", "QUOTA_EXHAUSTED" -> "Bạn đã dùng hết hạn mức token hôm nay. Chờ hạn mức được đặt lại hoặc chọn API cá nhân.";
            case "AI_QUOTA_GLOBAL_EXCEEDED" -> "API chung của trường đã hết hạn mức token hôm nay. Chờ hạn mức được đặt lại hoặc chọn API cá nhân.";
            case "AI_QUOTA_BLOCKED" -> "Tài khoản này chưa được phép dùng API chung. Hãy chọn API cá nhân hoặc liên hệ quản trị viên.";
            case "AI_QUOTA_NOT_CONFIGURED" -> "API chung chưa được cấu hình hạn mức. Hãy chọn API cá nhân hoặc liên hệ quản trị viên.";
            case "AI_SERVICE_UNAVAILABLE", "AI service unavailable" -> "Không thể kết nối dịch vụ AI. Vui lòng thử lại sau.";
            case "RUBRIC_CHANGED" -> "Rubric đã thay đổi. Hãy tải lại bài chấm trước khi tạo đề xuất AI.";
            case "SUBMISSION_CHANGED", "NOT_LATEST_ATTEMPT", "Task correlation mismatch" -> "Kết quả AI không còn khớp với bài nộp hiện tại. Hãy tải lại và chấm lại bằng AI.";
            case "AI proposal cannot be applied to this submission" -> "Không thể áp dụng kết quả AI cho bài nộp hiện tại. Hãy tải lại và thử lại.";
            case "AI task failed", "AI task unavailable or failed" -> fallback;
            default -> coded || diagnostic.matches("[A-Z][A-Z0-9_]+") ? fallback : diagnostic;
        };
    }
}
