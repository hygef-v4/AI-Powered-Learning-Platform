package edu.aiplatform.quizpractice.service;

import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.academics.contract.IClassAccess;
import edu.aiplatform.jobs.contract.IScheduledScanner;
import edu.aiplatform.quizpractice.repository.IQuizAttemptRepository;
import edu.aiplatform.quizpractice.model.QuizAttempt;
import edu.aiplatform.quizpractice.model.SubmitMode;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Tự nộp bằng câu trả lời đã lưu (BR-U11-14, NFR-U11-03); worker chạy mỗi phút.
 * <ul>
 *   <li>Lượt đã quá {@code deadlineAt} cộng 30 giây: {@code AUTO_TIME_LIMIT}.</li>
 *   <li>Lượt người học không còn làm được vì lớp không còn {@code OPEN} hoặc đã rời lớp: {@code AUTO_ACCESS_LOST}.
 *       Lưu và nộp tay đều cần lớp mở và còn ghi danh, nên không có bước này thì lượt không giới hạn giờ treo mãi.</li>
 * </ul>
 * Mỗi lượt một transaction: một lượt chấm lỗi chỉ để lại lượt đó cho lần chạy sau, không rollback cả lô. Nộp có
 * điều kiện trạng thái nên chạy lại hoặc chạy song song không nộp hai lần. U05 không trả lời thì bỏ qua lớp đó, không
 * nộp oan. Mỗi lần chạy nộp tối đa {@link #BATCH} lượt mỗi loại (quá hạn, mất quyền làm); phần còn lại để lần chạy sau.
 */
@Component
public class QuizAutoSubmitScanner implements IScheduledScanner {

    private static final Logger log = LoggerFactory.getLogger(QuizAutoSubmitScanner.class);
    static final int BATCH = 100;

    private final IQuizAttemptRepository attempts;
    private final IClassAccess classes;
    private final PracticeSupport support;
    private final TransactionOperations transactions;
    private final int batch;

    QuizAutoSubmitScanner(IQuizAttemptRepository attempts, IClassAccess classes, PracticeSupport support,
                          TransactionOperations transactions) {
        this(attempts, classes, support, transactions, BATCH);
    }

    QuizAutoSubmitScanner(IQuizAttemptRepository attempts, IClassAccess classes, PracticeSupport support,
                          TransactionOperations transactions, int batch) {
        this.attempts = attempts;
        this.classes = classes;
        this.support = support;
        this.transactions = transactions;
        this.batch = batch;
    }

    @Override
    public String name() {
        return "quiz-auto-submit";
    }

    @Override
    public void scan(Instant now) {
        for (QuizAttempt overdue : attempts.findOverdue(now.minus(QuizAttempt.GRACE), batch)) {
            submitIf(overdue.id(), attempt -> attempt.isOverdueAt(now), SubmitMode.AUTO_TIME_LIMIT);
        }
        int budget = batch;
        for (UUID classId : attempts.listClassesWithInProgress()) {
            if (budget <= 0) {
                return;
            }
            budget -= submitStranded(classId, budget);
        }
    }

    /**
     * Nộp tối đa {@code limit} lượt đang làm của lớp mà người học không còn làm tiếp được; trả số lượt đã thử nộp.
     * Đọc lượt đang làm trước rồi mới đọc lớp và ghi danh: lượt bắt đầu sau khi chụp danh sách ghi danh sẽ không bị
     * nộp oan, vì lúc đó nó chưa nằm trong danh sách lượt.
     */
    private int submitStranded(UUID classId, int limit) {
        List<QuizAttempt> inProgress = attempts.listInProgressByClass(classId);
        if (inProgress.isEmpty()) {
            return 0;
        }
        Predicate<UUID> stranded;
        try {
            Optional<ClassRef> ref = classes.getClassRef(classId);
            if (ref.isEmpty()) {
                return 0;
            }
            if (ref.get().status() != ClassStatus.OPEN) {
                stranded = accountId -> true;
            } else {
                Set<UUID> students = new HashSet<>(classes.listActiveStudents(classId));
                stranded = accountId -> !students.contains(accountId);
            }
        } catch (RuntimeException unavailable) {
            log.warn("Chưa kiểm được lớp {} để tự nộp lượt quiz: {}", classId, unavailable.getClass().getSimpleName());
            return 0;
        }
        int tried = 0;
        for (QuizAttempt attempt : inProgress) {
            if (tried >= limit) {
                break;
            }
            if (stranded.test(attempt.accountId())) {
                submitIf(attempt.id(), QuizAttempt::hasInProgressStatus, SubmitMode.AUTO_ACCESS_LOST);
                tried++;
            }
        }
        return tried;
    }

    private void submitIf(UUID attemptId, Predicate<QuizAttempt> condition, SubmitMode mode) {
        try {
            transactions.executeWithoutResult(status -> attempts.findAttempt(attemptId)
                    .filter(condition)
                    .ifPresent(attempt -> support.submit(null, attempt, mode)));
        } catch (RuntimeException e) {
            log.warn("Tự nộp lượt quiz {} lỗi: {}", attemptId, e.getClass().getSimpleName());
        }
    }
}
