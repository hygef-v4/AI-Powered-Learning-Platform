package edu.aiplatform.quizpractice.domain;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Bảng {@code quiz_attempts}; chỉ dùng trong U11. Lưu, nộp là câu lệnh có điều kiện (NFR-U11-10). */
public interface IQuizAttemptRepository {

    Optional<QuizAttempt> find(UUID id);

    /** Khóa theo (Quiz, người học) tới hết transaction để bắt đầu lượt không trùng (NFR-U11-11). */
    void lockStart(UUID quizId, UUID accountId);

    Optional<QuizAttempt> findInProgress(UUID quizId, UUID accountId);

    int countAttempts(UUID quizId, UUID accountId);

    void insert(QuizAttempt attempt);

    /** Lưu câu trả lời khi lượt còn {@code IN_PROGRESS} và {@code answer_version} còn bằng {@code expectedVersion}. */
    boolean saveAnswers(QuizAttempt attempt, long expectedVersion);

    /** Nộp khi lượt còn {@code IN_PROGRESS}; chạy lại không nộp hai lần. */
    boolean submit(QuizAttempt attempt);

    /** Các lượt của một người học, mới trước (lịch sử, chi tiết Quiz). */
    List<QuizAttempt> listByAccount(UUID accountId, Collection<UUID> quizIds);

    /** Mọi Quiz người học đã có lượt, kèm lớp đã làm. */
    Map<UUID, UUID> quizzesAttemptedBy(UUID accountId);

    /** Lượt đang làm đã quá hạn cộng ân hạn (scanner). */
    List<QuizAttempt> findOverdue(Instant cutoff, int limit);

    /** Lượt đang làm của một Quiz (Quiz bị ngưng hoặc có version mới). */
    List<QuizAttempt> findInProgressByQuiz(UUID quizId);

    /** Lượt của sinh viên một lớp cho một Quiz (giảng viên xem). */
    List<QuizAttempt> listByQuizAndClass(UUID quizId, UUID classId);
}
