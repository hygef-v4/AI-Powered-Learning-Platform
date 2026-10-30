package edu.aiplatform.quizpractice.application;

import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.quizpractice.application.PracticeViews.ResultView;
import edu.aiplatform.quizpractice.application.PracticeViews.SaveResult;
import edu.aiplatform.quizpractice.application.PracticeViews.TakingQuestion;
import edu.aiplatform.quizpractice.application.PracticeViews.TakingView;
import edu.aiplatform.quizpractice.domain.AttemptSnapshot;
import edu.aiplatform.quizpractice.domain.IQuizAttemptRepository;
import edu.aiplatform.quizpractice.domain.QuizAttempt;
import edu.aiplatform.quizpractice.domain.SubmitMode;
import edu.aiplatform.quizzes.contract.QuizQuestionView;
import edu.aiplatform.quizzes.contract.QuizStatus;
import edu.aiplatform.quizzes.contract.QuizStudentView;
import edu.aiplatform.quizzes.contract.QuizView;
import edu.aiplatform.shared.error.ApiException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Làm Quiz (UC 51, 52; F3–F5, F7): bắt đầu, đề, tự lưu, nộp tay, kết quả. Chỉ sinh viên đang học lớp (R5). */
@Service
public class QuizAttemptService {

    private static final SecureRandom SEEDS = new SecureRandom();

    private final IQuizAttemptRepository attempts;
    private final PracticeSupport support;
    private final PracticeAccess access;
    private final Clock clock;

    public QuizAttemptService(IQuizAttemptRepository attempts, PracticeSupport support, PracticeAccess access,
                              Clock clock) {
        this.attempts = attempts;
        this.support = support;
        this.access = access;
        this.clock = clock;
    }

    /**
     * Bắt đầu lượt mới, hoặc mở lại lượt đang làm (BR-U11-10). Lớp phải đang mở, Quiz đang mở trong lớp, còn lượt.
     * Khóa theo (Quiz, người học) để hai lần bấm không tạo hai lượt (NFR-U11-11).
     */
    @Transactional
    public TakingView start(Actor actor, UUID quizId, UUID classId) {
        access.requireClass(actor, Actions.QUIZ_PRACTICE, classId, "QUIZ", quizId, PracticeSupport.QUIZ_NOT_FOUND);
        ClassRef ref = support.classRef(classId);
        QuizView quiz = support.quizInClass(quizId, ref);
        attempts.lockStart(quizId, actor.accountId());
        var existing = attempts.findInProgress(quizId, actor.accountId());
        if (existing.isPresent()) {
            return taking(existing.get());
        }
        if (ref.status() != ClassStatus.OPEN || quiz.status() != QuizStatus.OPEN) {
            access.rejected(actor, "QUIZ_SUBMIT_REJECTED", null, "QUIZ_NOT_OPEN");
            throw ApiException.conflict("QUIZ_NOT_OPEN", "Quiz đã ngưng hoặc lớp không còn mở");
        }
        int used = attempts.countAttempts(quizId, actor.accountId());
        Integer max = quiz.settings().maxAttempts();
        if (max != null && used >= max) {
            access.rejected(actor, "QUIZ_SUBMIT_REJECTED", null, "NO_ATTEMPTS_LEFT");
            throw ApiException.conflict("NO_ATTEMPTS_LEFT", "Bạn đã dùng hết " + max + "/" + max + " lượt làm quiz này");
        }
        QuizStudentView view = support.studentView(quizId);
        QuizAttempt attempt = QuizAttempt.start(quizId, classId, actor.accountId(), used + 1,
                AttemptSnapshot.of(view, SEEDS.nextLong()), clock.instant());
        attempts.insert(attempt);
        return taking(attempt, view);
    }

    /** Đề của lượt đang làm, không có đáp án (BR-U11-12, NFR-U11-30). */
    @Transactional(readOnly = true)
    public TakingView get(Actor actor, UUID attemptId) {
        QuizAttempt attempt = support.ownAttempt(actor, attemptId, Actions.QUIZ_PRACTICE);
        if (!attempt.inProgress()) {
            throw ApiException.conflict("ATTEMPT_SUBMITTED", "Lượt này đã nộp; hãy xem kết quả");
        }
        return taking(attempt);
    }

    /**
     * Tự lưu (BR-U11-12): lượt của mình, đang làm, chưa quá hạn cộng 30 giây; câu và lựa chọn phải thuộc đề;
     * {@code answerVersion} lệch thì {@code 409}. Không log câu trả lời (NFR-U11-31).
     */
    @Transactional
    public SaveResult saveAnswers(Actor actor, UUID attemptId, Map<UUID, List<String>> answers, long answerVersion) {
        QuizAttempt attempt = support.ownAttempt(actor, attemptId, Actions.QUIZ_PRACTICE);
        Instant now = clock.instant();
        if (!attempt.inProgress()) {
            throw ApiException.conflict("ATTEMPT_SUBMITTED", "Lượt này đã nộp");
        }
        if (!attempt.acceptsAnswersAt(now)) {
            throw ApiException.conflict("ATTEMPT_TIME_UP", "Đã hết thời gian làm bài");
        }
        Map<UUID, List<String>> clean = new LinkedHashMap<>();
        for (var entry : answers.entrySet()) {
            List<String> options = entry.getValue().stream().distinct().toList();
            if (!attempt.snapshot().contains(entry.getKey(), options)) {
                throw ApiException.badRequest("ANSWER_INVALID", "Câu hoặc lựa chọn không thuộc đề của lượt này");
            }
            if (!options.isEmpty()) {
                clean.put(entry.getKey(), options);
            }
        }
        if (attempt.answerVersion() != answerVersion) {
            throw versionConflict();
        }
        QuizAttempt saved = attempt.withAnswers(clean, now);
        if (!attempts.saveAnswers(saved, answerVersion)) {
            throw versionConflict();
        }
        return new SaveResult(saved.answerVersion(), now);
    }

    /** Nộp tay (BR-U11-13): nộp lặp trả kết quả cũ; quá hạn cộng 30 giây thì từ chối, scanner sẽ tự nộp. */
    @Transactional
    public ResultView submit(Actor actor, UUID attemptId) {
        QuizAttempt attempt = support.ownAttempt(actor, attemptId, Actions.QUIZ_PRACTICE);
        if (attempt.inProgress()) {
            if (!attempt.acceptsAnswersAt(clock.instant())) {
                access.rejected(actor, "QUIZ_SUBMIT_REJECTED", attemptId, "ATTEMPT_TIME_UP");
                throw ApiException.conflict("ATTEMPT_TIME_UP", "Đã hết thời gian làm bài; lượt sẽ được tự nộp");
            }
            attempt = support.submit(actor, attempt, SubmitMode.MANUAL);
        }
        return support.result(attempt, false, canRetry(actor, attempt));
    }

    /** Kết quả lượt đã nộp của mình (UC 52, BR-U11-20). */
    @Transactional(readOnly = true)
    public ResultView result(Actor actor, UUID attemptId) {
        QuizAttempt attempt = support.ownAttempt(actor, attemptId, Actions.QUIZ_PRACTICE);
        if (attempt.inProgress()) {
            throw ApiException.conflict("ATTEMPT_IN_PROGRESS", "Lượt này chưa nộp");
        }
        return support.result(attempt, false, canRetry(actor, attempt));
    }

    private boolean canRetry(Actor actor, QuizAttempt attempt) {
        if (!access.allowedClass(actor, Actions.QUIZ_PRACTICE, attempt.classId())) {
            return false;
        }
        ClassRef ref = support.classRef(attempt.classId());
        if (ref.status() != ClassStatus.OPEN) {
            return false;
        }
        QuizView quiz = support.quizInClass(attempt.quizId(), ref);
        if (quiz.status() != QuizStatus.OPEN) {
            return false;
        }
        Integer max = quiz.settings().maxAttempts();
        return max == null || attempts.countAttempts(attempt.quizId(), attempt.accountId()) < max;
    }

    private TakingView taking(QuizAttempt attempt) {
        return taking(attempt, support.studentView(attempt.quizId()));
    }

    private TakingView taking(QuizAttempt attempt, QuizStudentView view) {
        Map<UUID, QuizQuestionView> byId = view.questions().stream()
                .collect(Collectors.toMap(QuizQuestionView::questionId, Function.identity()));
        List<TakingQuestion> questions = attempt.snapshot().questionOrder().stream()
                .filter(byId::containsKey)
                .map(id -> {
                    QuizQuestionView q = byId.get(id);
                    return new TakingQuestion(id, q.type(), q.stem(), q.points(), PracticeSupport.ordered(q, attempt));
                })
                .toList();
        return new TakingView(attempt.id(), attempt.quizId(), attempt.classId(), attempt.snapshot().title(),
                attempt.attemptNo(), attempt.startedAt(), attempt.deadlineAt(), clock.instant(),
                attempt.answerVersion(), questions, attempt.answers(), attempt.snapshot().activityMonitoringEnabled(),
                attempt.snapshot().activityNotice());
    }

    private static ApiException versionConflict() {
        return ApiException.conflict("ANSWER_VERSION_CONFLICT", "Câu trả lời đã được lưu từ nơi khác; hãy tải lại");
    }
}
