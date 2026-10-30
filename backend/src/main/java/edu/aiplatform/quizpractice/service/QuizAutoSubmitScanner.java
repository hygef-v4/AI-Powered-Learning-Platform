package edu.aiplatform.quizpractice.service;

import edu.aiplatform.jobs.contract.IScheduledScanner;
import edu.aiplatform.quizpractice.repository.IQuizAttemptRepository;
import edu.aiplatform.quizpractice.model.QuizAttempt;
import edu.aiplatform.quizpractice.model.SubmitMode;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Tự nộp lượt đã quá {@code deadlineAt} cộng 30 giây bằng câu trả lời đã lưu (BR-U11-14, NFR-U11-03); worker chạy mỗi
 * phút. Mỗi lượt một transaction: một lượt chấm lỗi chỉ để lại lượt đó cho lần chạy sau, không rollback cả lô. Nộp có
 * điều kiện trạng thái nên chạy lại hoặc chạy song song không nộp hai lần.
 */
@Component
public class QuizAutoSubmitScanner implements IScheduledScanner {

    private static final Logger log = LoggerFactory.getLogger(QuizAutoSubmitScanner.class);
    static final int BATCH = 100;

    private final IQuizAttemptRepository attempts;
    private final PracticeSupport support;
    private final TransactionOperations transactions;

    QuizAutoSubmitScanner(IQuizAttemptRepository attempts, PracticeSupport support,
                          TransactionOperations transactions) {
        this.attempts = attempts;
        this.support = support;
        this.transactions = transactions;
    }

    @Override
    public String name() {
        return "quiz-auto-submit";
    }

    @Override
    public void scan(Instant now) {
        for (QuizAttempt overdue : attempts.findOverdue(now.minus(QuizAttempt.GRACE), BATCH)) {
            try {
                transactions.executeWithoutResult(status -> attempts.find(overdue.id())
                        .filter(attempt -> attempt.overdueAt(now))
                        .ifPresent(attempt -> support.submit(null, attempt, SubmitMode.AUTO_TIME_LIMIT)));
            } catch (RuntimeException e) {
                log.warn("Tự nộp lượt quiz {} lỗi: {}", overdue.id(), e.getClass().getSimpleName());
            }
        }
    }
}
