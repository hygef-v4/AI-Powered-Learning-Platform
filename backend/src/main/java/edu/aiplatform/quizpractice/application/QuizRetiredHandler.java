package edu.aiplatform.quizpractice.application;

import edu.aiplatform.quizpractice.domain.IQuizAttemptRepository;
import edu.aiplatform.quizpractice.domain.QuizAttempt;
import edu.aiplatform.quizpractice.domain.SubmitMode;
import edu.aiplatform.quizzes.contract.IQuizLifecycleListener;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cài {@link IQuizLifecycleListener} của U10: Quiz bị ngưng hoặc có version mới được phát hành thì tự nộp và chấm
 * mọi lượt đang làm ({@code AUTO_RETIRED}) trong cùng transaction của U10 (BR-U11-14, NFR-U11-10).
 */
@Component
public class QuizRetiredHandler implements IQuizLifecycleListener {

    private final IQuizAttemptRepository attempts;
    private final PracticeSupport support;

    QuizRetiredHandler(IQuizAttemptRepository attempts, PracticeSupport support) {
        this.attempts = attempts;
        this.support = support;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void onRetired(UUID quizId) {
        for (QuizAttempt attempt : attempts.findInProgressByQuiz(quizId)) {
            support.submit(null, attempt, SubmitMode.AUTO_RETIRED);
        }
    }
}
