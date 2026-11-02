package edu.aiplatform.quizpractice.domain;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Một lượt làm Quiz (UC 51). {@code IN_PROGRESS → SUBMITTED}; lưu câu trả lời kèm {@code answerVersion}, trước
 * {@code deadlineAt} cộng 30 giây ân hạn (BR-U11-12, 13). Đã nộp thì câu trả lời và kết quả không đổi (BR-U11-15).
 */
public record QuizAttempt(UUID id, UUID quizId, UUID classId, UUID accountId, int attemptNo, AttemptSnapshot snapshot,
                          Map<UUID, List<String>> answers, long answerVersion, AttemptStatus status,
                          SubmitMode submitMode, Instant startedAt, Instant deadlineAt, Instant lastSavedAt,
                          Instant submittedAt, BigDecimal score, BigDecimal maxScore, Integer correctCount,
                          List<QuestionResult> result) {

    /** Lần lưu cuối và nộp tay vẫn nhận trong 30 giây sau hạn (bù độ trễ mạng). */
    public static final Duration GRACE = Duration.ofSeconds(30);

    public QuizAttempt {
        answers = answers == null ? Map.of() : Map.copyOf(answers);
        result = result == null ? null : List.copyOf(result);
    }

    public static QuizAttempt start(UUID quizId, UUID classId, UUID accountId, int attemptNo, AttemptSnapshot snapshot,
                                    Instant now) {
        Integer limit = snapshot.settings().timeLimitMinutes();
        Instant deadline = limit == null ? null : now.plus(Duration.ofMinutes(limit));
        return new QuizAttempt(UUID.randomUUID(), quizId, classId, accountId, attemptNo, snapshot, Map.of(), 0,
                AttemptStatus.IN_PROGRESS, null, now, deadline, null, null, null, null, null, null);
    }

    public boolean inProgress() {
        return status == AttemptStatus.IN_PROGRESS;
    }

    /** Còn nhận lưu và nộp tay: chưa nộp, chưa quá hạn cộng ân hạn. */
    public boolean acceptsAnswersAt(Instant now) {
        return inProgress() && (deadlineAt == null || !now.isAfter(deadlineAt.plus(GRACE)));
    }

    /** Quá hạn cộng ân hạn: scanner tự nộp. */
    public boolean overdueAt(Instant now) {
        return inProgress() && deadlineAt != null && now.isAfter(deadlineAt.plus(GRACE));
    }

    public QuizAttempt withAnswers(Map<UUID, List<String>> newAnswers, Instant now) {
        if (!inProgress()) {
            throw new IllegalStateException("Lượt đã nộp");
        }
        return new QuizAttempt(id, quizId, classId, accountId, attemptNo, snapshot, newAnswers, answerVersion + 1,
                status, submitMode, startedAt, deadlineAt, now, submittedAt, score, maxScore, correctCount, result);
    }

    public QuizAttempt submitted(SubmitMode mode, BigDecimal newScore, BigDecimal newMax, List<QuestionResult> results,
                                 Instant now) {
        if (!inProgress()) {
            throw new IllegalStateException("Lượt đã nộp");
        }
        int correct = (int) results.stream().filter(QuestionResult::correct).count();
        return new QuizAttempt(id, quizId, classId, accountId, attemptNo, snapshot, answers, answerVersion,
                AttemptStatus.SUBMITTED, mode, startedAt, deadlineAt, lastSavedAt, now, newScore, newMax, correct, results);
    }
}
