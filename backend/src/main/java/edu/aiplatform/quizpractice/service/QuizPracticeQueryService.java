package edu.aiplatform.quizpractice.service;


import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.academics.contract.IClassAccess;
import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.quizpractice.dto.PracticeViews.PracticeQuizDetail;
import edu.aiplatform.quizpractice.dto.PracticeViews.PracticeQuizRow;
import edu.aiplatform.quizpractice.repository.IQuizAttemptRepository;
import edu.aiplatform.quizpractice.model.QuizAttempt;
import edu.aiplatform.quizzes.contract.IQuizQuery;
import edu.aiplatform.quizzes.contract.QuizStatus;
import edu.aiplatform.quizzes.contract.QuizView;
import edu.aiplatform.shared.error.ApiException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Xem Quiz Practice (UC 49, 50; F1, F2): lịch sử gộp các lớp đang học, chi tiết một Quiz trong một lớp. */
@Service
public class QuizPracticeQueryService {

    private final IQuizAttemptRepository attempts;
    private final IQuizQuery quizzes;
    private final IClassAccess classes;
    private final PracticeSupport support;
    private final PracticeAccess access;

    public QuizPracticeQueryService(IQuizAttemptRepository attempts, IQuizQuery quizzes, IClassAccess classes,
                                    PracticeSupport support, PracticeAccess access) {
        this.attempts = attempts;
        this.quizzes = quizzes;
        this.classes = classes;
        this.support = support;
        this.access = access;
    }

    /**
     * Quiz Practice History (BR-U11-02): Quiz đang mở của các lớp {@code OPEN} đang học và Quiz mình đã làm (kể cả
     * đã ngưng). {@code classId} lọc theo một lớp.
     */
    @Transactional(readOnly = true)
    public List<PracticeQuizRow> listHistory(Actor actor, UUID classId) {
        if (actor == null || actor.role() != Role.STUDENT) {
            throw ApiException.createForbidden("QUIZ_PRACTICE_STUDENT_ONLY");
        }
        List<ClassRef> open = PracticeSupport.call(() -> classes.listOpenClassesOf(actor.accountId())).stream()
                .filter(c -> c.status() == ClassStatus.OPEN)
                .filter(c -> classId == null || c.classId().equals(classId))
                .toList();
        Map<String, Entry> entries = new LinkedHashMap<>();
        for (ClassRef ref : open) {
            for (QuizView quiz : PracticeSupport.call(() -> quizzes.listForClass(ref.classId()))) {
                entries.putIfAbsent(buildKey(quiz.quizId(), ref.classId()), new Entry(quiz, ref));
            }
        }
        Map<UUID, ClassRef> openById = open.stream().collect(Collectors.toMap(ClassRef::classId, c -> c));
        for (var done : attempts.mapQuizzesAttemptedBy(actor.accountId()).entrySet()) {
            ClassRef ref = openById.get(done.getValue());
            if (ref == null || entries.containsKey(buildKey(done.getKey(), ref.classId()))) {
                continue;
            }
            PracticeSupport.call(() -> quizzes.loadQuiz(done.getKey()))
                    .ifPresent(quiz -> entries.put(buildKey(quiz.quizId(), ref.classId()), new Entry(quiz, ref)));
        }
        List<UUID> quizIds = entries.values().stream().map(e -> e.quiz.quizId()).distinct().toList();
        Map<UUID, List<QuizAttempt>> mine = attempts.listByAccount(actor.accountId(), quizIds).stream()
                .collect(Collectors.groupingBy(QuizAttempt::quizId));
        List<PracticeQuizRow> rows = new ArrayList<>();
        for (Entry entry : entries.values()) {
            List<QuizAttempt> own = mine.getOrDefault(entry.quiz.quizId(), List.of()).stream()
                    .filter(a -> a.classId().equals(entry.ref.classId())).toList();
            rows.add(buildRow(entry.quiz, entry.ref, own));
        }
        return rows;
    }

    /** Quiz Practice Detail (BR-U11-03): chỉ sinh viên đang học lớp; Quiz đã ngưng chỉ xem khi mình đã có lượt. */
    @Transactional(readOnly = true)
    public PracticeQuizDetail loadQuiz(Actor actor, UUID quizId, UUID classId) {
        access.requireClass(actor, Actions.QUIZ_PRACTICE, classId, PracticeSupport.QUIZ_NOT_FOUND);
        ClassRef ref = support.loadClassRef(classId);
        QuizView quiz = support.loadQuizInClass(quizId, ref);
        List<QuizAttempt> own = attempts.listByAccount(actor.accountId(), List.of(quizId)).stream()
                .filter(a -> a.classId().equals(classId)).toList();
        if (quiz.status() != QuizStatus.OPEN && own.isEmpty()) {
            throw ApiException.createNotFound(PracticeSupport.QUIZ_NOT_FOUND);
        }
        var view = support.loadStudentView(quizId);
        boolean showScore = quiz.settings().showScoreAfterSubmit();
        Integer max = quiz.settings().maxAttempts();
        int used = own.size();
        UUID inProgress = own.stream().filter(QuizAttempt::hasInProgressStatus).map(QuizAttempt::id).findFirst().orElse(null);
        String blocked = resolveBlockedReason(quiz, ref, used, inProgress);
        return new PracticeQuizDetail(quizId, classId, ref.code(), ref.subjectCode(), quiz.title(),
                view.instructions(), quiz.status(), view.questionCount(), quiz.maxScore(),
                quiz.settings().timeLimitMinutes(), used, max, max == null ? null : Math.max(max - used, 0),
                findBestScore(own), showScore, quiz.settings().showCorrectAnswers(),
                quiz.activityMonitoringEnabled() ? quiz.activityNotice() : null,
                quiz.activityMonitoringEnabled() ? quiz.settings().leavePenalty() : null,
                own.stream().sorted(Comparator.comparingInt(QuizAttempt::attemptNo).reversed())
                        .map(a -> PracticeSupport.buildRow(a, a.snapshot().settings().showScoreAfterSubmit())).toList(),
                inProgress, blocked == null, blocked, view.instructionsDocument());
    }

    private static PracticeQuizRow buildRow(QuizView quiz, ClassRef ref, List<QuizAttempt> own) {
        QuizAttempt last = own.stream().filter(a -> !a.hasInProgressStatus())
                .max(Comparator.comparing(QuizAttempt::submittedAt).thenComparingInt(QuizAttempt::attemptNo)).orElse(null);
        BigDecimal lastScore = last != null && last.snapshot() != null && last.snapshot().settings().showScoreAfterSubmit()
                ? last.score() : null;
        UUID inProgress = own.stream().filter(QuizAttempt::hasInProgressStatus).map(QuizAttempt::id).findFirst().orElse(null);
        Integer max = quiz.settings().maxAttempts();
        return new PracticeQuizRow(quiz.quizId(), ref.classId(), ref.code(), ref.subjectCode(), quiz.title(), quiz.status(),
                quiz.settings().timeLimitMinutes(), own.size(), max, findBestScore(own),
                lastScore, quiz.maxScore(),
                own.stream().map(QuizAttempt::startedAt).max(Comparator.naturalOrder()).orElse(null), inProgress,
                resolveBlockedReason(quiz, ref, own.size(), inProgress) == null);
    }

    /** Lý do không làm được (BR-U11-03); lượt đang làm thì vẫn "Tiếp tục" được. */
    private static String resolveBlockedReason(QuizView quiz, ClassRef ref, int used, UUID inProgress) {
        if (inProgress != null) {
            return null;
        }
        if (quiz.status() != QuizStatus.OPEN) {
            return "Quiz đã ngưng; bạn chỉ xem lại được các lần đã làm.";
        }
        if (ref.status() != ClassStatus.OPEN) {
            return "Lớp không còn mở.";
        }
        Integer max = quiz.settings().maxAttempts();
        if (max != null && used >= max) {
            return "Bạn đã dùng hết " + max + "/" + max + " lượt làm quiz này.";
        }
        return null;
    }

    private static BigDecimal findBestScore(List<QuizAttempt> attempts) {
        return attempts.stream()
                .filter(a -> a.snapshot() != null && a.snapshot().settings().showScoreAfterSubmit())
                .map(QuizAttempt::score)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    private static String buildKey(UUID quizId, UUID classId) {
        return quizId + "/" + classId;
    }

    private record Entry(QuizView quiz, ClassRef ref) {
    }
}
