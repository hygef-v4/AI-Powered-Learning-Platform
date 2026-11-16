package edu.aiplatform.quizpractice.service;


import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.quizpractice.dto.PracticeViews.ResultView;
import edu.aiplatform.quizpractice.dto.PracticeViews.SaveResult;
import edu.aiplatform.quizpractice.dto.PracticeViews.TakingQuestion;
import edu.aiplatform.quizpractice.dto.PracticeViews.TakingView;
import edu.aiplatform.quizpractice.model.AttemptSnapshot;
import edu.aiplatform.quizpractice.repository.IQuizAttemptRepository;
import edu.aiplatform.quizpractice.model.QuizAttempt;
import edu.aiplatform.quizpractice.model.SubmitMode;
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
import java.util.Set;
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
     * Khóa Quiz trước khi đọc đề, rồi (Quiz, người học), tới commit (NFR-U11-11).
     */
    @Transactional
    public TakingView start(Actor actor, UUID quizId, UUID classId) {
        access.requireClass(actor, Actions.QUIZ_PRACTICE, classId, "QUIZ", quizId, PracticeSupport.QUIZ_NOT_FOUND);
        ClassRef ref = support.loadClassRef(classId);
        attempts.lockStart(quizId, actor.accountId());
        QuizView quiz = support.loadQuizInClass(quizId, ref);
        var existing = attempts.findInProgress(quizId, actor.accountId(), classId);
        if (existing.isPresent()) {
            return buildTakingView(existing.get());
        }
        quiz = support.loadQuizInClass(quizId, ref);
        if (ref.status() != ClassStatus.OPEN || quiz.status() != QuizStatus.OPEN) {
            access.recordRejected(actor, "QUIZ_SUBMIT_REJECTED", null, "QUIZ_NOT_OPEN");
            throw ApiException.createConflict("QUIZ_NOT_OPEN", "Quiz đã ngưng hoặc lớp không còn mở");
        }
        int used = attempts.countAttempts(quizId, actor.accountId(), classId);
        Integer max = quiz.settings().maxAttempts();
        if (max != null && used >= max) {
            access.recordRejected(actor, "QUIZ_SUBMIT_REJECTED", null, "NO_ATTEMPTS_LEFT");
            throw ApiException.createConflict("NO_ATTEMPTS_LEFT", "Bạn đã dùng hết " + max + "/" + max + " lượt làm quiz này");
        }
        Set<UUID> seen = attempts.listByAccount(actor.accountId(), List.of(quizId)).stream()
                .flatMap(a -> a.snapshot().questionOrder().stream()).collect(Collectors.toSet());
        long seed = SEEDS.nextLong();
        QuizStudentView view = support.createPaper(quizId, seen, seed);
        int nextAttemptNo = attempts.listByAccount(actor.accountId(), List.of(quizId)).stream()
                .filter(a -> a.classId().equals(classId)).mapToInt(QuizAttempt::attemptNo).max().orElse(0) + 1;
        QuizAttempt attempt = QuizAttempt.start(quizId, classId, actor.accountId(), nextAttemptNo,
                AttemptSnapshot.createSnapshot(view, seed), clock.instant());
        attempts.insert(attempt);
        return buildTakingView(attempt, view);
    }

    /** Đề của lượt đang làm, không có đáp án (BR-U11-12, NFR-U11-30). */
    @Transactional(readOnly = true)
    public TakingView loadAttempt(Actor actor, UUID attemptId) {
        QuizAttempt attempt = support.loadOwnActiveAttempt(actor, attemptId, Actions.QUIZ_PRACTICE);
        if (!attempt.hasInProgressStatus()) {
            throw ApiException.createConflict("ATTEMPT_SUBMITTED", "Lượt này đã nộp; hãy xem kết quả");
        }
        return buildTakingView(attempt);
    }

    /**
     * Tự lưu (BR-U11-12): lượt của mình, đang làm, chưa quá hạn cộng 30 giây; câu và lựa chọn phải thuộc đề;
     * {@code answerVersion} lệch thì {@code 409}. Không log câu trả lời (NFR-U11-31).
     */
    @Transactional
    public SaveResult saveAnswers(Actor actor, UUID attemptId, Map<UUID, List<String>> answers, long answerVersion) {
        QuizAttempt attempt = support.loadOwnActiveAttempt(actor, attemptId, Actions.QUIZ_PRACTICE);
        Instant now = clock.instant();
        if (!attempt.hasInProgressStatus()) {
            throw ApiException.createConflict("ATTEMPT_SUBMITTED", "Lượt này đã nộp");
        }
        if (!attempt.acceptsAnswersAt(now)) {
            throw ApiException.createConflict("ATTEMPT_TIME_UP", "Đã hết thời gian làm bài");
        }
        Map<UUID, List<String>> clean = new LinkedHashMap<>();
        for (var entry : answers.entrySet()) {
            List<String> options = entry.getValue().stream().distinct().toList();
            if (!attempt.snapshot().contains(entry.getKey(), options)) {
                throw ApiException.createBadRequest("ANSWER_INVALID", "Câu hoặc lựa chọn không thuộc đề của lượt này");
            }
            if (!options.isEmpty()) {
                clean.put(entry.getKey(), options);
            }
        }
        if (attempt.answerVersion() != answerVersion) {
            throw createVersionConflictError();
        }
        QuizAttempt saved = attempt.replaceAnswers(clean, now);
        if (!attempts.saveAnswers(saved, answerVersion)) {
            throw createVersionConflictError();
        }
        return new SaveResult(saved.answerVersion(), now);
    }

    /** Nộp tay (BR-U11-13): nộp lặp trả kết quả cũ; quá hạn cộng 30 giây thì từ chối, scanner sẽ tự nộp. */
    @Transactional
    public ResultView submit(Actor actor, UUID attemptId) {
        QuizAttempt attempt = support.loadOwnActiveAttempt(actor, attemptId, Actions.QUIZ_PRACTICE);
        if (attempt.hasInProgressStatus()) {
            if (!attempt.acceptsAnswersAt(clock.instant())) {
                access.recordRejected(actor, "QUIZ_SUBMIT_REJECTED", attemptId, "ATTEMPT_TIME_UP");
                throw ApiException.createConflict("ATTEMPT_TIME_UP", "Đã hết thời gian làm bài; lượt sẽ được tự nộp");
            }
            attempt = support.submit(actor, attempt, SubmitMode.MANUAL);
        }
        return support.buildResult(attempt, false, canRetry(actor, attempt));
    }

    /** Kết quả lượt đã nộp của mình (UC 52, BR-U11-20). */
    @Transactional(readOnly = true)
    public ResultView loadResult(Actor actor, UUID attemptId) {
        QuizAttempt attempt = support.loadOwnAttempt(actor, attemptId, Actions.QUIZ_PRACTICE);
        if (attempt.hasInProgressStatus()) {
            throw ApiException.createConflict("ATTEMPT_IN_PROGRESS", "Lượt này chưa nộp");
        }
        return support.buildResult(attempt, false, canRetry(actor, attempt));
    }

    private boolean canRetry(Actor actor, QuizAttempt attempt) {
        if (!access.isClassAllowed(actor, Actions.QUIZ_PRACTICE, attempt.classId())) {
            return false;
        }
        ClassRef ref = support.loadClassRef(attempt.classId());
        if (ref.status() != ClassStatus.OPEN) {
            return false;
        }
        QuizView quiz = support.loadQuizInClass(attempt.quizId(), ref);
        if (quiz.status() != QuizStatus.OPEN) {
            return false;
        }
        Integer max = quiz.settings().maxAttempts();
        return max == null || attempts.countAttempts(attempt.quizId(), attempt.accountId(), attempt.classId()) < max;
    }

    private TakingView buildTakingView(QuizAttempt attempt) {
        return buildTakingView(attempt, support.loadPaper(attempt));
    }

    private TakingView buildTakingView(QuizAttempt attempt, QuizStudentView view) {
        Map<UUID, QuizQuestionView> byId = view.questions().stream()
                .collect(Collectors.toMap(QuizQuestionView::questionId, Function.identity()));
        List<TakingQuestion> questions = attempt.snapshot().questionOrder().stream()
                .filter(byId::containsKey)
                .map(id -> {
                    QuizQuestionView q = byId.get(id);
                    return new TakingQuestion(id, q.type(), q.stem(), q.points(), PracticeSupport.orderOptions(q, attempt));
                })
                .toList();
        return new TakingView(attempt.id(), attempt.quizId(), attempt.classId(), attempt.snapshot().title(),
                attempt.attemptNo(), attempt.startedAt(), attempt.deadlineAt(), clock.instant(),
                attempt.answerVersion(), questions, attempt.answers(), attempt.snapshot().activityMonitoringEnabled(),
                attempt.snapshot().activityNotice(), attempt.snapshot().settings().leavePenalty());
    }

    private static ApiException createVersionConflictError() {
        return ApiException.createConflict("ANSWER_VERSION_CONFLICT", "Câu trả lời đã được lưu từ nơi khác; hãy tải lại");
    }
}
