package edu.aiplatform.quizzes.domain;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Đề xuất câu từ AI hoặc MCP (P4): tách khỏi Quiz và ngân hàng, chỉ lệnh chấp nhận mới thêm câu.
 * {@code provenance} giữ người yêu cầu, nguồn, provider, model, tài liệu, nhãn agent, {@code requestRef}; khi lỗi
 * thêm {@code failureCode}, {@code failureMessage} (không có cột riêng để không phải thêm migration).
 */
public record QuizAiDraft(UUID id, DraftTarget target, UUID quizId, UUID subjectId, DraftOrigin origin, UUID taskId,
                          DraftStatus status, List<DraftQuestion> proposal, Map<String, Object> provenance,
                          Long baseVersion, UUID createdBy, Instant createdAt, Instant decidedAt) {

    public static final String REQUEST_REF = "requestRef";
    public static final String FAILURE_CODE = "failureCode";
    public static final String FAILURE_MESSAGE = "failureMessage";

    public QuizAiDraft {
        proposal = proposal == null ? List.of() : List.copyOf(proposal);
        provenance = provenance == null ? Map.of() : Map.copyOf(provenance);
    }

    /** Đề xuất chờ task AI; {@code quizId} trống khi đích là ngân hàng. */
    public static QuizAiDraft pending(DraftTarget target, UUID quizId, UUID subjectId, Long baseVersion,
                                      Map<String, Object> provenance, UUID createdBy, Instant now) {
        return new QuizAiDraft(UUID.randomUUID(), target, quizId, subjectId, DraftOrigin.AI_TASK, null,
                DraftStatus.PENDING, List.of(), provenance, baseVersion, createdBy, now, null);
    }

    /** Đề xuất do agent ghi qua MCP, sẵn sàng để người soạn xem (F13 bước 2). */
    public static QuizAiDraft fromMcp(UUID quizId, UUID subjectId, long baseVersion, List<DraftQuestion> questions,
                                      Map<String, Object> provenance, UUID createdBy, Instant now) {
        return new QuizAiDraft(UUID.randomUUID(), DraftTarget.QUIZ, quizId, subjectId, DraftOrigin.MCP, null,
                DraftStatus.READY, questions, provenance, baseVersion, createdBy, now, null);
    }

    public String requestRef() {
        Object value = provenance.get(REQUEST_REF);
        return value == null ? null : value.toString();
    }

    public QuizAiDraft ready(List<DraftQuestion> questions, Map<String, Object> extraProvenance) {
        return new QuizAiDraft(id, target, quizId, subjectId, origin, taskId, DraftStatus.READY, questions,
                merged(extraProvenance), baseVersion, createdBy, createdAt, null);
    }

    public QuizAiDraft failed(String code, String message, Instant now) {
        Map<String, Object> failure = new LinkedHashMap<>();
        failure.put(FAILURE_CODE, code);
        if (message != null) {
            failure.put(FAILURE_MESSAGE, message);
        }
        return new QuizAiDraft(id, target, quizId, subjectId, origin, taskId, DraftStatus.FAILED, List.of(),
                merged(failure), baseVersion, createdBy, createdAt, now);
    }

    public QuizAiDraft decided(DraftStatus decision, Instant now) {
        return new QuizAiDraft(id, target, quizId, subjectId, origin, taskId, decision, proposal, provenance,
                baseVersion, createdBy, createdAt, now);
    }

    private Map<String, Object> merged(Map<String, Object> extra) {
        Map<String, Object> result = new LinkedHashMap<>(provenance);
        if (extra != null) {
            result.putAll(extra);
        }
        return result;
    }
}
