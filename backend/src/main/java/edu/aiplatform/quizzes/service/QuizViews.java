package edu.aiplatform.quizzes.service;

import edu.aiplatform.quizzes.service.QuestionViews.QuestionView;
import edu.aiplatform.quizzes.contract.QuizScope;
import edu.aiplatform.quizzes.contract.QuizSettings;
import edu.aiplatform.quizzes.contract.QuizStatus;
import edu.aiplatform.quizzes.repository.IQuizRepository.QuizSummary;
import edu.aiplatform.quizzes.model.Difficulty;
import edu.aiplatform.quizzes.model.DrawRule;
import edu.aiplatform.quizzes.model.Quiz;
import edu.aiplatform.quizzes.model.QuizOrigin;
import edu.aiplatform.quizzes.model.QuizPaperMode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import edu.aiplatform.shared.content.FormattedText;

/** Dữ liệu Quiz cho người soạn (giảng viên, Chủ nhiệm môn); có đáp án, không dùng cho người học (P2). */
public final class QuizViews {

    private QuizViews() {
    }

    public record QuizInfo(UUID id, QuizScope scope, UUID classId, UUID subjectId, UUID sourceQuizId,
                           QuizOrigin origin, int versionNo, String title, String instructions, QuizSettings settings,
                           boolean activityMonitoringEnabled, String activityNotice, QuizPaperMode paperMode,
                           QuizStatus status, Instant publishedAt, Instant retiredAt, String retiredReason,
                           Instant createdAt, Instant updatedAt, long version, FormattedText instructionsDocument) {

        static QuizInfo of(Quiz q) {
            return new QuizInfo(q.id(), q.scope(), q.classId(), q.subjectId(), q.sourceQuizId(), q.origin(),
                    q.versionNo(), q.title(), q.instructions(), q.settings(), q.activityMonitoringEnabled(),
                    q.activityNotice(), q.paperMode(), q.status(), q.publishedAt(), q.retiredAt(),
                    q.retiredReason(), q.createdAt(), q.updatedAt(), q.version(), q.instructionsDocument());
        }
    }

    /** Một câu trong Quiz: thứ tự, điểm, câu riêng của Quiz hay câu ngân hàng, phiên bản câu đã ghim. */
    public record QuizItemView(int position, BigDecimal points, boolean inline, QuestionView question) {
    }

    /** Một quy tắc sinh đề (BR-U10-17). */
    public record DrawRuleView(int position, String tag, Difficulty difficulty, int questionCount, BigDecimal pointsEach) {

        static DrawRuleView of(DrawRule r) {
            return new DrawRuleView(r.position(), r.tag(), r.difficulty(), r.questionCount(), r.pointsEach());
        }
    }

    /**
     * Quiz Detail; {@code canManage} sai khi giảng viên lớp xem Quiz của môn (chỉ xem, BR-U10-10). Quiz sinh đề có
     * {@code drawRules}, còn {@code maxScore} tính theo quy tắc.
     */
    public record QuizDetail(QuizInfo quiz, List<QuizItemView> items, List<DrawRuleView> drawRules, BigDecimal maxScore,
                             boolean canManage) {
    }

    /** Một thẻ trong ngân hàng của môn kèm số câu đang dùng, tổng và theo độ khó, để chọn khi lập quy tắc. */
    public record BankTag(String tag, int activeCount, Map<Difficulty, Integer> byDifficulty) {
    }

    /** Một câu trong đề mẫu của Quiz sinh đề, kèm quy tắc đã bốc ra câu đó. */
    public record PaperPreviewItem(int rulePosition, BigDecimal points, QuestionView question) {
    }

    /** Một quy tắc còn thiếu câu: cần {@code required}, kho chỉ cấp được {@code available}. */
    public record RuleShortage(int position, String tag, Difficulty difficulty, int required, int available) {
    }

    /** "Xem thử đề mẫu": đề bốc thử và các quy tắc thiếu câu; không lưu gì. */
    public record PaperPreview(List<PaperPreviewItem> items, BigDecimal maxScore, List<RuleShortage> shortages) {
    }

    /** Một dòng Quiz trên Quiz List hoặc tab Đánh giá; {@code readOnly} với Quiz của môn hiện ở lớp. */
    public record QuizRow(UUID id, QuizScope scope, UUID classId, UUID subjectId, int versionNo, String title,
                          QuizStatus status, int itemCount, BigDecimal maxScore, Integer timeLimitMinutes,
                          Instant updatedAt, boolean readOnly) {

        static QuizRow of(QuizSummary s, boolean readOnly) {
            Quiz q = s.quiz();
            return new QuizRow(q.id(), q.scope(), q.classId(), q.subjectId(), q.versionNo(), q.title(), q.status(),
                    s.itemCount(), s.maxScore(), q.settings().timeLimitMinutes(), q.updatedAt(), readOnly);
        }
    }

    public record QuizPage(List<QuizRow> items, long total, int page, int size) {
    }

    /** Kết quả bốc thử (BR-U10-14): câu đề xuất và số câu phù hợp bộ lọc. */
    public record RandomPreview(List<QuestionView> questions, long matching, int freeSlots) {
    }
}
