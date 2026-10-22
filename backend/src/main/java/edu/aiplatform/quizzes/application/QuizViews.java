package edu.aiplatform.quizzes.application;

import edu.aiplatform.quizzes.application.QuestionViews.QuestionView;
import edu.aiplatform.quizzes.contract.QuizScope;
import edu.aiplatform.quizzes.contract.QuizSettings;
import edu.aiplatform.quizzes.contract.QuizStatus;
import edu.aiplatform.quizzes.domain.IQuizRepository.QuizSummary;
import edu.aiplatform.quizzes.domain.Quiz;
import edu.aiplatform.quizzes.domain.QuizOrigin;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Dữ liệu Quiz cho người soạn (giảng viên, Chủ nhiệm môn); có đáp án, không dùng cho người học (P2). */
public final class QuizViews {

    private QuizViews() {
    }

    public record QuizInfo(UUID id, QuizScope scope, UUID classId, UUID subjectId, UUID sourceQuizId,
                           QuizOrigin origin, int versionNo, String title, String instructions, QuizSettings settings,
                           boolean activityMonitoringEnabled, String activityNotice, List<UUID> referenceMaterialIds,
                           QuizStatus status, Instant publishedAt, Instant retiredAt, String retiredReason,
                           Instant createdAt, Instant updatedAt, long version) {

        static QuizInfo of(Quiz q) {
            return new QuizInfo(q.id(), q.scope(), q.classId(), q.subjectId(), q.sourceQuizId(), q.origin(),
                    q.versionNo(), q.title(), q.instructions(), q.settings(), q.activityMonitoringEnabled(),
                    q.activityNotice(), q.referenceMaterialIds(), q.status(), q.publishedAt(), q.retiredAt(),
                    q.retiredReason(), q.createdAt(), q.updatedAt(), q.version());
        }
    }

    /** Một câu trong Quiz: thứ tự, điểm, câu riêng của Quiz hay câu ngân hàng, phiên bản câu đã ghim. */
    public record QuizItemView(int position, BigDecimal points, boolean inline, QuestionView question) {
    }

    /** Quiz Detail; {@code canManage} sai khi giảng viên lớp xem Quiz của môn (chỉ xem, BR-U10-10). */
    /** {@code activityRetentionConfigured}: đã đặt thời hạn lưu nên bật được ghi nhận hoạt động (K3). */
    public record QuizDetail(QuizInfo quiz, List<QuizItemView> items, BigDecimal maxScore, boolean canManage,
                             boolean activityRetentionConfigured) {
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
