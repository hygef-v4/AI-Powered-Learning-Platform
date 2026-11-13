package edu.aiplatform.quizpractice.service;

import edu.aiplatform.quizpractice.repository.IQuizAttemptRepository;
import edu.aiplatform.quizzes.contract.IQuizAttemptPresence;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Cài {@link IQuizAttemptPresence} của U10 từ bảng {@code quiz_attempts}. */
@Component
class QuizAttemptPresence implements IQuizAttemptPresence {

    private final IQuizAttemptRepository attempts;

    QuizAttemptPresence(IQuizAttemptRepository attempts) {
        this.attempts = attempts;
    }

    @Override
    public boolean hasAttempts(UUID quizId) {
        return attempts.existsForQuiz(quizId);
    }
}
