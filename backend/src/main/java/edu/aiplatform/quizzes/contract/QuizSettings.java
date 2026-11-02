package edu.aiplatform.quizzes.contract;

/**
 * {@code timeLimitMinutes} rỗng là không giới hạn; {@code maxAttempts} rỗng là không giới hạn; {@code leavePenalty} rỗng
 * là không gợi ý trừ điểm khi rời trang.
 */
public record QuizSettings(boolean shuffleQuestions, boolean shuffleOptions, Integer timeLimitMinutes,
                           Integer maxAttempts, boolean showScoreAfterSubmit, boolean showCorrectAnswers,
                           LeavePenalty leavePenalty) {

    public QuizSettings(boolean shuffleQuestions, boolean shuffleOptions, Integer timeLimitMinutes,
                        Integer maxAttempts, boolean showScoreAfterSubmit, boolean showCorrectAnswers) {
        this(shuffleQuestions, shuffleOptions, timeLimitMinutes, maxAttempts, showScoreAfterSubmit, showCorrectAnswers,
                null);
    }
}
