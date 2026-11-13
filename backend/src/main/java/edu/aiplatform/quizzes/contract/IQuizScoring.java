package edu.aiplatform.quizzes.contract;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Chấm theo đáp án ghim mà đáp án không rời U10; hiện đáp án sau khi nộp theo cài đặt. Dùng bởi U11. Đáp án của Quiz
 * đã phát hành cố định như đề ({@link IQuizQuery#getStudentView}), nên chấm lại hay xem lại sau này cho cùng kết quả.
 */
public interface IQuizScoring {

    QuizScore score(UUID quizId, List<QuizAnswer> answers);

    AnswerReview answerReview(UUID quizId);

    /** Chấm lượt sinh đề: chỉ các câu và điểm đã lưu trong lượt (BR-U10-17). */
    default QuizScore score(UUID quizId, List<QuizAnswer> answers, Map<UUID, BigDecimal> questionPoints) {
        return score(quizId, answers);
    }

    /** Đáp án của các câu trong lượt sinh đề. */
    default AnswerReview answerReview(UUID quizId, Collection<UUID> questionIds) {
        return answerReview(quizId);
    }
}
