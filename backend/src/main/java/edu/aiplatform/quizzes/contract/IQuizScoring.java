package edu.aiplatform.quizzes.contract;

import java.util.List;
import java.util.UUID;

/**
 * Chấm theo đáp án ghim mà đáp án không rời U10; hiện đáp án sau khi nộp theo cài đặt. Dùng bởi U11. Đáp án của Quiz
 * đã phát hành cố định như đề ({@link IQuizQuery#getStudentView}), nên chấm lại hay xem lại sau này cho cùng kết quả.
 */
public interface IQuizScoring {

    QuizScore score(UUID quizId, List<QuizAnswer> answers);

    AnswerReview answerReview(UUID quizId);
}
