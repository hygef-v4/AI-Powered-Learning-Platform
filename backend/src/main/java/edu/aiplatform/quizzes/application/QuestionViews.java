package edu.aiplatform.quizzes.application;

import edu.aiplatform.quizzes.contract.QuestionType;
import edu.aiplatform.quizzes.domain.Difficulty;
import edu.aiplatform.quizzes.domain.Question;
import edu.aiplatform.quizzes.domain.QuestionContent;
import edu.aiplatform.quizzes.domain.QuestionOption;
import edu.aiplatform.quizzes.domain.QuestionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Dữ liệu ngân hàng câu hỏi trả cho người soạn (Chủ nhiệm môn); có đáp án, không dùng cho người học (P2). */
public final class QuestionViews {

    private QuestionViews() {
    }

    public record QuestionView(UUID id, UUID lineageId, int versionNo, UUID subjectId, QuestionType type, String stem,
                               List<QuestionOption> options, List<String> correctOptionIds, String explanation,
                               BigDecimal defaultPoints, Difficulty difficulty, List<String> tags,
                               QuestionStatus status, Instant activatedAt, UUID createdBy, Instant createdAt,
                               Instant updatedAt, long version) {

        static QuestionView of(Question q) {
            QuestionContent c = q.content();
            return new QuestionView(q.id(), q.lineageId(), q.versionNo(), q.subjectId(), c.type(), c.stem(),
                    c.options(), c.correctOptionIds(), c.explanation(), c.defaultPoints(), c.difficulty(), c.tags(),
                    q.status(), q.activatedAt(), q.createdBy(), q.createdAt(), q.updatedAt(), q.version());
        }
    }

    public record VersionEntry(UUID id, int versionNo, QuestionStatus status, Instant createdAt, Instant activatedAt) {
    }

    /** Question Detail: câu, lịch sử phiên bản, số Quiz đang ghim, id bản nháp của lineage (nếu có và khác câu này). */
    public record QuestionDetail(QuestionView question, List<VersionEntry> versions, int usedInQuizzes, UUID draftId) {
    }

    public record QuestionRow(UUID id, UUID lineageId, int versionNo, QuestionType type, String stem,
                              Difficulty difficulty, List<String> tags, QuestionStatus status, boolean hasDraft,
                              Instant updatedAt) {
    }

    public record QuestionPage(List<QuestionRow> items, long total, int page, int size) {
    }
}
