package edu.aiplatform.quizzes.service;

import edu.aiplatform.quizzes.model.Question;
import edu.aiplatform.quizzes.repository.IQuestionRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Điền dấu vân tay cho câu có từ trước khi thêm cột (BR-U10-08). Chuẩn hóa làm trong Java nên không làm được bằng SQL
 * migration. Chạy lại không sao: chỉ lấy dòng còn trống; lỗi thì ghi log, lần khởi động sau làm tiếp.
 */
@Component
class QuestionFingerprintBackfill {

    private static final Logger log = LoggerFactory.getLogger(QuestionFingerprintBackfill.class);
    private static final int BATCH = 500;

    private final IQuestionRepository questions;
    private final TransactionOperations transactions;

    QuestionFingerprintBackfill(IQuestionRepository questions, TransactionOperations transactions) {
        this.questions = questions;
        this.transactions = transactions;
    }

    @EventListener(ApplicationReadyEvent.class)
    void run() {
        try {
            int done = 0;
            for (;;) {
                List<Question> batch = questions.withoutFingerprint(BATCH);
                if (batch.isEmpty()) {
                    break;
                }
                transactions.executeWithoutResult(status -> batch.forEach(questions::saveFingerprint));
                done += batch.size();
            }
            if (done > 0) {
                log.info("Filled question fingerprints: {}", done);
            }
        } catch (RuntimeException failure) {
            log.warn("Question fingerprint backfill stopped; it resumes on next start", failure);
        }
    }
}
