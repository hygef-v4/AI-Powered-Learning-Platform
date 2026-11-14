package edu.aiplatform.quizpractice.dto;

import edu.aiplatform.assignmentwork.contract.ActivitySummary;
import edu.aiplatform.quizpractice.model.ActivityOutcome;
import edu.aiplatform.quizpractice.model.ActivitySignal;
import edu.aiplatform.quizpractice.model.AttemptStatus;
import edu.aiplatform.quizpractice.model.PenaltyDecision;
import edu.aiplatform.quizpractice.model.SubmitMode;
import edu.aiplatform.quizzes.contract.LeavePenalty;
import edu.aiplatform.quizzes.contract.QuestionType;
import edu.aiplatform.quizzes.contract.QuizOptionView;
import edu.aiplatform.quizzes.contract.QuizStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Dữ liệu U11 trả cho người học và giảng viên. Đáp án chỉ có trong kết quả, theo cài đặt (NFR-U11-30). */
public final class PracticeViews {

    private PracticeViews() {
    }

    /** Một Quiz trên Quiz Practice History (BR-U11-02); điểm trống khi Quiz không hiện điểm. */
    public record PracticeQuizRow(UUID quizId, UUID classId, String classCode, String subjectCode, String title,
                                  QuizStatus status, Integer timeLimitMinutes, int attemptsUsed, Integer maxAttempts,
                                  BigDecimal bestScore, BigDecimal lastScore, BigDecimal maxScore,
                                  Instant lastAttemptAt, UUID inProgressAttemptId, boolean canStart) {
    }

    /**
     * Một lượt trên Quiz Practice Detail hoặc danh sách của giảng viên; {@code leaveCount}, {@code activitySignal},
     * {@code suggestedPenalty}, {@code penaltyDecision} chỉ có ở danh sách của giảng viên.
     */
    public record AttemptRow(UUID attemptId, int attemptNo, AttemptStatus status, Instant startedAt,
                             Instant submittedAt, BigDecimal score, BigDecimal maxScore, Integer leaveCount,
                             ActivitySignal activitySignal, BigDecimal suggestedPenalty,
                             PenaltyDecision penaltyDecision) {
    }

    /**
     * Điểm gốc và quyết định trừ điểm của lượt (BR-U11-33). {@code suggestedPenalty} chỉ giảng viên thấy; sinh viên chỉ
     * thấy điểm đã bị trừ, lý do và việc lượt đang chờ quyết định.
     */
    public record PenaltyView(BigDecimal rawScore, BigDecimal suggestedPenalty, BigDecimal penaltyPoints,
                              Integer leaveCount, boolean decisionPending, PenaltyDecision decision,
                              String decisionReason) {

        public static PenaltyView of(ActivityOutcome outcome, boolean teacher) {
            return outcome == null ? null : new PenaltyView(outcome.rawScore(),
                    teacher ? outcome.suggestedPenalty() : null, outcome.penaltyPoints(), outcome.leaveCount(),
                    outcome.pendingDecision(), outcome.decision(), outcome.decisionReason());
        }
    }

    /** Quiz Practice Detail (BR-U11-03): {@code blockedReason} giải thích vì sao không làm được. */
    public record PracticeQuizDetail(UUID quizId, UUID classId, String classCode, String subjectCode, String title,
                                     String instructions, QuizStatus status, int questionCount, BigDecimal maxScore,
                                     Integer timeLimitMinutes, int attemptsUsed, Integer maxAttempts,
                                     Integer attemptsLeft, BigDecimal bestScore, boolean showScoreAfterSubmit,
                                     boolean showCorrectAnswers, String activityNotice, LeavePenalty leavePenalty,
                                     List<AttemptRow> attempts, UUID inProgressAttemptId, boolean canStart,
                                     String blockedReason,
                                     edu.aiplatform.shared.content.FormattedText instructionsDocument) {
    }

    /** Câu khi đang làm: lựa chọn theo thứ tự đã trộn, không có đáp án. */
    public record TakingQuestion(UUID questionId, QuestionType type, String stem, BigDecimal points,
                                 List<QuizOptionView> options) {
    }

    /** Quiz Taking (BR-U11-12): {@code serverTime} để đồng hồ phía trình duyệt bù lệch giờ. */
    /** {@code activityMonitoringEnabled}, {@code activityNotice} lấy từ ảnh chụp của lượt (K4). */
    public record TakingView(UUID attemptId, UUID quizId, UUID classId, String title, int attemptNo,
                             Instant startedAt, Instant deadlineAt, Instant serverTime, long answerVersion,
                             List<TakingQuestion> questions, Map<UUID, List<String>> answers,
                             boolean activityMonitoringEnabled, String activityNotice, LeavePenalty leavePenalty) {

        public TakingView(UUID attemptId, UUID quizId, UUID classId, String title, int attemptNo, Instant startedAt,
                          Instant deadlineAt, Instant serverTime, long answerVersion, List<TakingQuestion> questions,
                          Map<UUID, List<String>> answers, boolean activityMonitoringEnabled, String activityNotice) {
            this(attemptId, quizId, classId, title, attemptNo, startedAt, deadlineAt, serverTime, answerVersion, questions,
                    answers, activityMonitoringEnabled, activityNotice, null);
        }
    }

    public record SaveResult(long answerVersion, Instant savedAt) {
    }

    /** Một câu trong kết quả: lựa chọn của mình, đúng sai; đáp án, giải thích chỉ khi được hiện. */
    public record ResultQuestion(UUID questionId, QuestionType type, String stem, List<QuizOptionView> options,
                                 List<String> selectedOptionIds, Boolean correct, BigDecimal points,
                                 BigDecimal maxPoints, List<String> correctOptionIds, String explanation) {
    }

    /**
     * Quiz Result (BR-U11-20): điểm trống khi Quiz không hiện điểm; đúng/sai từng câu và số câu đúng, sai trống khi Quiz
     * không hiện cả điểm lẫn đáp án. {@code penalty} trống khi không hiện điểm hoặc Quiz không bật theo dõi.
     */
    public record ResultView(UUID attemptId, UUID quizId, UUID classId, String title, int attemptNo,
                             Instant startedAt, Instant submittedAt, SubmitMode submitMode, boolean showScore,
                             boolean showAnswers, BigDecimal score, BigDecimal maxScore, Integer correctCount,
                             Integer wrongCount, int blankCount, List<ResultQuestion> questions, boolean canRetry,
                             PenaltyView penalty) {
    }

    /** Tóm tắt lượt của một sinh viên cho giảng viên (BR-U11-31); {@code studentCode} là MSSV (BR-U11-42). */
    public record StudentAttempts(UUID accountId, String displayName, String email, int attempts, BigDecimal bestScore,
                                  BigDecimal lastScore, Instant lastSubmittedAt, List<AttemptRow> rows,
                                  Integer lastLeaveCount, boolean signalWarning, boolean penaltyPending,
                                  String studentCode) {
    }

    public record ClassAttempts(UUID quizId, UUID classId, Integer maxAttempts, int studentsAttempted,
                                int totalStudents, List<StudentAttempts> students) {
    }

    /** Một lượt cho giảng viên: kết quả đầy đủ chỉ đọc kèm tóm tắt hoạt động (có thể trống). */
    public record TeacherAttemptDetail(ResultView result, String displayName, ActivitySummary activity,
                                       ActivitySignal activitySignal) {
    }
}
