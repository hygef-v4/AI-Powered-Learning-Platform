package edu.aiplatform.quizzes.contract;

/** {@code timeLimitMinutes} rỗng là không giới hạn; {@code maxAttempts} rỗng là không giới hạn. */
public record QuizSettings(boolean shuffleQuestions, boolean shuffleOptions, Integer timeLimitMinutes,
                           Integer maxAttempts, boolean showScoreAfterSubmit, boolean showCorrectAnswers) {
}
