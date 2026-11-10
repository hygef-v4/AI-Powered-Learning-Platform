package edu.aiplatform.assignments.service;

import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.ai.contract.AiSource;
import edu.aiplatform.assignments.contract.DiagramType;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.GradingMode;
import edu.aiplatform.assignments.contract.RubricCriterion;
import edu.aiplatform.assignments.contract.SubmissionMode;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Đầu vào của các thao tác U07; web dựng từ body, service kiểm nghiệp vụ. */
public final class AssignmentCommands {

    private AssignmentCommands() {
    }

    public record Create(SubmissionMode submissionMode, GradingMode gradingMode, boolean diagramEnabled, String title) {
    }

    /** PATCH: trường {@code null} giữ nguyên; {@code activityNotice} rỗng là xóa thông báo. */
    public record Details(long version, String title, String instructions, Boolean diagramEnabled,
                          GradingMode gradingMode, Map<DiagramType, Integer> requiredDiagrams,
                          List<UUID> referenceMaterialIds, Boolean activityMonitoringEnabled, String activityNotice) {
    }

    /** {@code instructions} {@code null} giữ hướng dẫn cũ; {@code fromDocx} để audit việc nhập DOCX. */
    public record Skeleton(long version, DocumentModel skeleton, String instructions, boolean fromDocx) {
    }

    public record Publish(long version, Instant opensAt, boolean openNow, Instant closesAt, Instant lateUntil,
                          Integer maxAttempts, boolean confirmUngroupedStudents) {
    }

    public record Schedule(long version, Instant opensAt, Instant closesAt, Instant lateUntil, Integer maxAttempts) {
    }

    public record Retire(long version, String reason) {
    }

    public record RubricUpdate(long version, List<RubricCriterion> criteria) {
    }

    public record DraftOptions(String profile, String language, Integer questionCount, BigDecimal targetPoints) {
    }

    /**
     * {@code source} rỗng: hệ thống tự chọn nguồn theo token của lượt này; {@code PERSONAL} không kèm provider: chỉ thử
     * API cá nhân. {@code formatText}: chữ của mẫu đề (DOCX đã nhập ở trình duyệt, không giới hạn 2 MB) mà AI phải
     * theo (yêu cầu 2026-11-05). {@code mode} {@code REVISE}: AI sửa đề đang có theo {@code request}, bỏ qua tài
     * liệu nguồn, mẫu đề và tùy chọn tạo đề (yêu cầu 2026-11-09).
     */
    public record AiDraftRequest(String request, List<UUID> materialIds, List<String> sourceFileRefs,
                                 AiSource source, AiProvider provider, String requestRef, DraftOptions options,
                                 String formatText, String mode) {
        public AiDraftRequest(String request, List<UUID> materialIds, List<String> sourceFileRefs, AiSource source,
                              AiProvider provider, String requestRef, DraftOptions options, String formatText) {
            this(request, materialIds, sourceFileRefs, source, provider, requestRef, options, formatText, null);
        }

        public AiDraftRequest(String request, List<UUID> materialIds, List<String> sourceFileRefs, AiSource source,
                              AiProvider provider, String requestRef, DraftOptions options) {
            this(request, materialIds, sourceFileRefs, source, provider, requestRef, options, null, null);
        }

        public AiDraftRequest(String request, List<UUID> materialIds, AiSource source, AiProvider provider,
                              String requestRef) {
            this(request, materialIds, List.of(), source, provider, requestRef, null, null, null);
        }
    }

    public record ApplyDraft(long version, boolean replaceInstructions, boolean replaceTitle) {
        public ApplyDraft(long version, boolean replaceInstructions) {
            this(version, replaceInstructions, false);
        }
    }
}
