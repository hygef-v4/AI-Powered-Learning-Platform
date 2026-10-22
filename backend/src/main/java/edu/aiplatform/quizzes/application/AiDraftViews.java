package edu.aiplatform.quizzes.application;

import edu.aiplatform.quizzes.domain.DraftOrigin;
import edu.aiplatform.quizzes.domain.DraftQuestion;
import edu.aiplatform.quizzes.domain.DraftStatus;
import edu.aiplatform.quizzes.domain.DraftTarget;
import edu.aiplatform.quizzes.domain.QuizAiDraft;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** DTO đề xuất câu cho người soạn (có đáp án, chỉ trả cho người có quyền sửa đích). */
public final class AiDraftViews {

    static final String SOURCE = "source";
    static final String PROVIDER = "provider";
    static final String MATERIALS = "materials";
    static final String AGENT_LABEL = "agentLabel";
    static final String REQUESTED_COUNT = "requestedCount";

    private AiDraftViews() {
    }

    public record MaterialRef(UUID id, String title) {
    }

    public record AiDraftView(UUID id, DraftTarget target, UUID quizId, UUID subjectId, DraftOrigin origin,
                              DraftStatus status, List<DraftQuestion> questions, Integer requestedCount, String source,
                              String provider, List<MaterialRef> materials, String agentLabel, Long baseVersion,
                              String failureCode, String failureMessage, UUID createdBy, Instant createdAt,
                              Instant decidedAt) {

        static AiDraftView of(QuizAiDraft d) {
            Map<String, Object> p = d.provenance();
            Object count = p.get(REQUESTED_COUNT);
            return new AiDraftView(d.id(), d.target(), d.quizId(), d.subjectId(), d.origin(), d.status(), d.proposal(),
                    count instanceof Number n ? n.intValue() : null, string(p, SOURCE), string(p, PROVIDER),
                    materials(p.get(MATERIALS)), string(p, AGENT_LABEL), d.baseVersion(),
                    string(p, QuizAiDraft.FAILURE_CODE), string(p, QuizAiDraft.FAILURE_MESSAGE), d.createdBy(),
                    d.createdAt(), d.decidedAt());
        }

        private static String string(Map<String, Object> provenance, String key) {
            Object value = provenance.get(key);
            return value == null ? null : value.toString();
        }

        private static List<MaterialRef> materials(Object value) {
            List<MaterialRef> result = new ArrayList<>();
            if (value instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> m && m.get("id") != null) {
                        result.add(new MaterialRef(UUID.fromString(m.get("id").toString()),
                                m.get("title") == null ? null : m.get("title").toString()));
                    }
                }
            }
            return result;
        }
    }

    /** Kết quả chấp nhận đề xuất vào ngân hàng: các câu {@code DRAFT} mới tạo. */
    public record BankAcceptResult(AiDraftView draft, List<UUID> questionIds) {
    }
}
