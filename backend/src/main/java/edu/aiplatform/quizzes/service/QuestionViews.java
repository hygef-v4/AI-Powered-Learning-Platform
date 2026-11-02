package edu.aiplatform.quizzes.service;

import edu.aiplatform.quizzes.contract.QuestionType;
import edu.aiplatform.quizzes.model.Difficulty;
import edu.aiplatform.quizzes.model.Question;
import edu.aiplatform.quizzes.model.QuestionContent;
import edu.aiplatform.quizzes.model.QuestionOption;
import edu.aiplatform.quizzes.model.QuestionStatus;
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

    /** Câu gần giống trong ngân hàng (BR-U10-08), chỉ để cảnh báo; {@code percent} từ 70 đến 100. */
    public record SimilarQuestion(UUID id, String stem, QuestionStatus status, int percent) {
    }

    /** Lần duyệt gần nhất của câu đề xuất (BR-U10-09). */
    public record ReviewInfo(String decision, String reason, Instant reviewedAt) {
    }

    /**
     * Question Detail: câu, lịch sử phiên bản, số Quiz đang ghim, id bản nháp của lineage (nếu có và khác câu này), câu
     * gần giống, lần duyệt gần nhất, tên các Quiz sinh đề thiếu câu sau khi ngưng câu này.
     */
    public record QuestionDetail(QuestionView question, List<VersionEntry> versions, int usedInQuizzes, UUID draftId,
                                 List<SimilarQuestion> similar, ReviewInfo review, List<String> quizzesShortOfQuestions) {

        public QuestionDetail(QuestionView question, List<VersionEntry> versions, int usedInQuizzes, UUID draftId) {
            this(question, versions, usedInQuizzes, draftId, List.of(), null, List.of());
        }
    }

    /** Câu giảng viên đề xuất ở một lớp. */
    public record ProposalView(QuestionView question, ReviewInfo review, List<SimilarQuestion> similar) {
    }

    /** Kết quả từng câu của thao tác hàng loạt; {@code code} và {@code message} trống khi thành công. */
    public record BulkItemResult(UUID id, boolean ok, String code, String message) {
    }

    public record BulkResult(List<BulkItemResult> items, int succeeded, int failed) {
    }

    /** Một nhóm câu giống hệt nhau trong ngân hàng. */
    public record DuplicateGroup(List<SimilarQuestion> questions) {
    }

    public record QuestionRow(UUID id, UUID lineageId, int versionNo, QuestionType type, String stem,
                              Difficulty difficulty, List<String> tags, QuestionStatus status, boolean hasDraft,
                              Instant updatedAt, long version) {
    }

    public record QuestionPage(List<QuestionRow> items, long total, int page, int size) {
    }
}
