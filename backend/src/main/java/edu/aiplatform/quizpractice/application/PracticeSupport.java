package edu.aiplatform.quizpractice.application;

import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.academics.contract.IClassAccess;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.quizpractice.application.PracticeViews.AttemptRow;
import edu.aiplatform.quizpractice.application.PracticeViews.ResultQuestion;
import edu.aiplatform.quizpractice.application.PracticeViews.ResultView;
import edu.aiplatform.quizpractice.domain.IQuizAttemptRepository;
import edu.aiplatform.quizpractice.domain.QuestionResult;
import edu.aiplatform.quizpractice.domain.QuizAttempt;
import edu.aiplatform.quizpractice.domain.SubmitMode;
import edu.aiplatform.quizzes.contract.AnswerKey;
import edu.aiplatform.quizzes.contract.IQuizQuery;
import edu.aiplatform.quizzes.contract.IQuizScoring;
import edu.aiplatform.quizzes.contract.QuizAnswer;
import edu.aiplatform.quizzes.contract.QuizOptionView;
import edu.aiplatform.quizzes.contract.QuizQuestionView;
import edu.aiplatform.quizzes.contract.QuizScope;
import edu.aiplatform.quizzes.contract.QuizScore;
import edu.aiplatform.quizzes.contract.QuizStudentView;
import edu.aiplatform.quizzes.contract.QuizView;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Phần dùng chung của U11: Quiz trong lớp, chủ lượt, chấm và nộp, dựng kết quả. */
@Component
class PracticeSupport {

    static final String QUIZ_NOT_FOUND = "QUIZ_NOT_FOUND";
    static final String ATTEMPT_NOT_FOUND = "QUIZ_ATTEMPT_NOT_FOUND";

    private final IQuizAttemptRepository attempts;
    private final IQuizQuery quizzes;
    private final IQuizScoring scoring;
    private final IClassAccess classes;
    private final PracticeAccess access;
    private final Clock clock;

    PracticeSupport(IQuizAttemptRepository attempts, IQuizQuery quizzes, IQuizScoring scoring, IClassAccess classes,
                    PracticeAccess access, Clock clock) {
        this.attempts = attempts;
        this.quizzes = quizzes;
        this.scoring = scoring;
        this.classes = classes;
        this.access = access;
        this.clock = clock;
    }

    ClassRef classRef(UUID classId) {
        try {
            return classes.getClassRef(classId).orElseThrow(() -> ApiException.notFound("CLASS_NOT_FOUND"));
        } catch (ApiException rejected) {
            throw rejected;
        } catch (RuntimeException unavailable) {
            throw ApiException.unavailable("CLASS_UNAVAILABLE", "Chưa kiểm tra được lớp, vui lòng thử lại");
        }
    }

    /** Quiz thuộc lớp: Quiz của lớp đó, hoặc Quiz của môn chứa lớp (BR-U11-01, BR-U10-25). Không có thì 404. */
    QuizView quizInClass(UUID quizId, ClassRef ref) {
        QuizView quiz = call(() -> quizzes.get(quizId)).orElseThrow(() -> ApiException.notFound(QUIZ_NOT_FOUND));
        boolean belongs = quiz.scope() == QuizScope.CLASS
                ? ref.classId().equals(quiz.classId())
                : ref.subjectId().equals(quiz.subjectId());
        if (!belongs) {
            throw ApiException.notFound(QUIZ_NOT_FOUND);
        }
        return quiz;
    }

    QuizStudentView studentView(UUID quizId) {
        return call(() -> quizzes.getStudentView(quizId)).orElseThrow(() -> ApiException.notFound(QUIZ_NOT_FOUND));
    }

    /** Lượt của chính người gọi; của người khác thì 404 và audit (BR-U11-22). */
    QuizAttempt ownAttempt(Actor actor, UUID attemptId, String action) {
        QuizAttempt attempt = attempts.find(attemptId).orElseThrow(() -> ApiException.notFound(ATTEMPT_NOT_FOUND));
        if (actor == null || !attempt.accountId().equals(actor.accountId())) {
            throw access.deny(actor, action, PracticeAccess.OBJECT, attemptId, ATTEMPT_NOT_FOUND);
        }
        return attempt;
    }

    /**
     * Chấm bằng đáp án ghim của U10 rồi nộp có điều kiện {@code IN_PROGRESS} (BR-U11-15). Lượt đã được nộp ở nơi
     * khác thì trả bản đã nộp, không nộp hai lần (NFR-U11-10).
     */
    QuizAttempt submit(Actor actor, QuizAttempt attempt, SubmitMode mode) {
        Instant now = clock.instant();
        List<QuizAnswer> answers = attempt.answers().entrySet().stream()
                .map(e -> new QuizAnswer(e.getKey(), new HashSet<>(e.getValue()))).toList();
        QuizScore score = call(() -> scoring.score(attempt.quizId(), answers));
        List<QuestionResult> results = score.questions().stream()
                .map(q -> new QuestionResult(q.questionId(), q.correct(), q.points(), q.maxPoints())).toList();
        QuizAttempt submitted = attempt.submitted(mode, score.total(), score.maxTotal(), results, now);
        if (!attempts.submit(submitted)) {
            return attempts.find(attempt.id()).orElseThrow(() -> ApiException.notFound(ATTEMPT_NOT_FOUND));
        }
        access.success(actor, "QUIZ_ATTEMPT_SUBMITTED", attempt.id(), Map.of("mode", mode.name(),
                "quizId", attempt.quizId().toString()));
        return submitted;
    }

    /**
     * Kết quả một lượt đã nộp; {@code full} (giảng viên) luôn có điểm và đáp án, người học thì theo cài đặt đã chụp
     * (BR-U11-20).
     */
    ResultView result(QuizAttempt attempt, boolean full, boolean canRetry) {
        boolean showScore = full || attempt.snapshot().settings().showScoreAfterSubmit();
        boolean showAnswers = full || attempt.snapshot().settings().showCorrectAnswers();
        Map<UUID, QuizQuestionView> byId = studentView(attempt.quizId()).questions().stream()
                .collect(Collectors.toMap(QuizQuestionView::questionId, Function.identity()));
        Map<UUID, QuestionResult> results = attempt.result().stream()
                .collect(Collectors.toMap(QuestionResult::questionId, Function.identity()));
        Map<UUID, AnswerKey> keys = showAnswers
                ? call(() -> scoring.answerReview(attempt.quizId())).answers().stream()
                        .collect(Collectors.toMap(AnswerKey::questionId, Function.identity()))
                : Map.of();
        int correct = 0;
        int blank = 0;
        List<ResultQuestion> questions = new ArrayList<>();
        for (UUID questionId : attempt.snapshot().questionOrder()) {
            QuizQuestionView q = byId.get(questionId);
            QuestionResult r = results.get(questionId);
            if (q == null || r == null) {
                continue;
            }
            List<String> selected = attempt.answers().getOrDefault(questionId, List.of());
            if (selected.isEmpty()) {
                blank++;
            } else if (r.correct()) {
                correct++;
            }
            AnswerKey key = keys.get(questionId);
            questions.add(new ResultQuestion(questionId, q.type(), q.stem(), ordered(q, attempt), selected, r.correct(),
                    showScore ? r.points() : null, showScore ? r.maxPoints() : null,
                    key == null ? null : List.copyOf(key.correctOptionIds()), key == null ? null : key.explanation()));
        }
        int wrong = questions.size() - correct - blank;
        return new ResultView(attempt.id(), attempt.quizId(), attempt.classId(), attempt.snapshot().title(),
                attempt.attemptNo(), attempt.startedAt(), attempt.submittedAt(), attempt.submitMode(), showScore,
                showAnswers, showScore ? attempt.score() : null, showScore ? attempt.maxScore() : null, correct, wrong,
                blank, questions, canRetry);
    }

    /** Lựa chọn theo thứ tự đã trộn của lượt. */
    static List<QuizOptionView> ordered(QuizQuestionView q, QuizAttempt attempt) {
        Map<String, QuizOptionView> byId = q.options().stream()
                .collect(Collectors.toMap(QuizOptionView::optionId, Function.identity()));
        return attempt.snapshot().optionOrder().getOrDefault(q.questionId(), List.of()).stream()
                .map(byId::get).filter(o -> o != null).toList();
    }

    static AttemptRow row(QuizAttempt a, boolean showScore) {
        return new AttemptRow(a.id(), a.attemptNo(), a.status(), a.startedAt(), a.submittedAt(),
                showScore ? a.score() : null, showScore ? a.maxScore() : null);
    }

    /** U10 lỗi thì từ chối (BR-U11-41), giữ nguyên lỗi nghiệp vụ. */
    static <T> T call(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (ApiException rejected) {
            throw rejected;
        } catch (RuntimeException unavailable) {
            throw ApiException.unavailable("QUIZ_UNAVAILABLE", "Chưa tải được quiz, vui lòng thử lại");
        }
    }
}
