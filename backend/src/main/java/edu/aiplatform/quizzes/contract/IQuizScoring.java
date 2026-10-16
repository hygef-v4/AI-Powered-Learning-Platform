package edu.aiplatform.quizzes.contract;

import java.util.List;
import java.util.UUID;

/** Chấm theo đáp án ghim mà đáp án không rời U10; hiện đáp án sau khi nộp theo cài đặt. Dùng bởi U11. */
public interface IQuizScoring {

    QuizScore score(UUID quizId, List<QuizAnswer> answers);

    AnswerReview answerReview(UUID quizId);
}
