package edu.aiplatform.quizpractice.application;

import edu.aiplatform.jobs.contract.IScheduledScanner;
import edu.aiplatform.quizpractice.domain.IQuizAttemptRepository;
import edu.aiplatform.quizpractice.domain.QuizAttempt;
import edu.aiplatform.quizpractice.domain.SubmitMode;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tự nộp lượt đã quá {@code deadlineAt} cộng 30 giây bằng câu trả lời đã lưu (BR-U11-14, NFR-U11-03); worker chạy mỗi
 * phút. Nộp có điều kiện trạng thái nên chạy lại hoặc chạy song song không nộp hai lần.
 */
@Component
public class QuizAutoSubmitScanner implements IScheduledScanner {

    static final int BATCH = 100;

    private final IQuizAttemptRepository attempts;
    private final PracticeSupport support;

    QuizAutoSubmitScanner(IQuizAttemptRepository attempts, PracticeSupport support) {
        this.attempts = attempts;
        this.support = support;
    }

    @Override
    public String name() {
        return "quiz-auto-submit";
    }

    @Override
    @Transactional
    public void scan(Instant now) {
        for (QuizAttempt attempt : attempts.findOverdue(now.minus(QuizAttempt.GRACE), BATCH)) {
            support.submit(null, attempt, SubmitMode.AUTO_TIME_LIMIT);
        }
    }
}
