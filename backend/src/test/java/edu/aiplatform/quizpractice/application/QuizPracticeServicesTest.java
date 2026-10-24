package edu.aiplatform.quizpractice.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.academics.contract.IClassAccess;
import edu.aiplatform.assignmentwork.contract.IActivityQuery;
import edu.aiplatform.assignmentwork.contract.IActivityWrite;
import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.identity.contract.AccountContact;
import edu.aiplatform.identity.contract.AccountStatus;
import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.AuthorizationDecision;
import edu.aiplatform.identity.contract.IAccountLookup;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.quizpractice.domain.AttemptStatus;
import edu.aiplatform.quizpractice.domain.IQuizAttemptRepository;
import edu.aiplatform.quizpractice.domain.QuizAttempt;
import edu.aiplatform.quizpractice.domain.SubmitMode;
import edu.aiplatform.quizzes.contract.AnswerKey;
import edu.aiplatform.quizzes.contract.AnswerReview;
import edu.aiplatform.quizzes.contract.IQuizQuery;
import edu.aiplatform.quizzes.contract.IQuizScoring;
import edu.aiplatform.quizzes.contract.QuestionScore;
import edu.aiplatform.quizzes.contract.QuestionType;
import edu.aiplatform.quizzes.contract.QuizAnswer;
import edu.aiplatform.quizzes.contract.QuizOptionView;
import edu.aiplatform.quizzes.contract.QuizQuestionView;
import edu.aiplatform.quizzes.contract.QuizScope;
import edu.aiplatform.quizzes.contract.QuizScore;
import edu.aiplatform.quizzes.contract.QuizSettings;
import edu.aiplatform.quizzes.contract.QuizStatus;
import edu.aiplatform.quizzes.contract.QuizStudentView;
import edu.aiplatform.quizzes.contract.QuizView;
import edu.aiplatform.shared.error.ApiException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/** UC 49–52 và phần giảng viên trên repository giả trong bộ nhớ (BR-U11-01…31, 40, 41). */
class QuizPracticeServicesTest {

    private final UUID subject = UUID.randomUUID();
    private final UUID classA = UUID.randomUUID();
    private final UUID classB = UUID.randomUUID();
    private final UUID quizId = UUID.randomUUID();
    private final UUID q1 = UUID.randomUUID();
    private final UUID q2 = UUID.randomUUID();
    private final Actor student = new Actor(UUID.randomUUID(), Role.STUDENT);
    private final Actor other = new Actor(UUID.randomUUID(), Role.STUDENT);
    private final Actor teacher = new Actor(UUID.randomUUID(), Role.TEACHER);
    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-20T08:00:00Z"));
    private final List<AuditEvent> audits = new ArrayList<>();
    private final InMemoryAttempts store = new InMemoryAttempts();
    private final IQuizQuery quizzes = mock(IQuizQuery.class);
    private final IQuizScoring scoring = mock(IQuizScoring.class);
    private final IClassAccess classes = mock(IClassAccess.class);
    private final IActivityWrite activityWrite = mock(IActivityWrite.class);
    private QuizSettings settings = new QuizSettings(false, false, 10, 2, true, true);
    private QuizStatus quizStatus = QuizStatus.OPEN;
    private boolean monitoring;
    private QuizAttemptService attemptsService;
    private QuizPracticeQueryService queries;
    private TeacherQuizAttemptService teacherService;
    private QuizAutoSubmitScanner scanner;
    private QuizRetiredHandler retired;
    private QuizActivityService activity;

    @BeforeEach
    void setup() {
        IAuthorization authorization = (actor, action, ref) -> {
            boolean allowed = Actions.QUIZ_PRACTICE.equals(action)
                    ? (actor.equals(student) || actor.equals(other)) && classA.equals(ref.id())
                    : actor.equals(teacher) && classA.equals(ref.id());
            return allowed ? AuthorizationDecision.allow() : AuthorizationDecision.deny("OUT_OF_SCOPE");
        };
        when(classes.getClassRef(classA)).thenReturn(Optional.of(classRef(classA, ClassStatus.OPEN)));
        when(classes.getClassRef(classB)).thenReturn(Optional.of(classRef(classB, ClassStatus.OPEN)));
        when(classes.listOpenClassesOf(student.accountId())).thenReturn(List.of(classRef(classA, ClassStatus.OPEN)));
        when(classes.listActiveStudents(classA)).thenReturn(List.of(student.accountId(), other.accountId()));
        when(quizzes.get(quizId)).thenAnswer(call -> Optional.of(quiz()));
        when(quizzes.getStudentView(quizId)).thenAnswer(call -> Optional.of(new QuizStudentView(quiz(), "Đọc kỹ", List.of(
                question(q1, QuestionType.MCQ_SINGLE), question(q2, QuestionType.MCQ_MULTI)))));
        when(quizzes.listForClass(classA)).thenAnswer(call -> quizStatus == QuizStatus.OPEN ? List.of(quiz()) : List.of());
        when(scoring.score(eq(quizId), anyList())).thenAnswer(call -> score(call.getArgument(1)));
        when(scoring.answerReview(quizId)).thenReturn(new AnswerReview(quizId, List.of(
                new AnswerKey(q1, Set.of("1"), "Vì A"), new AnswerKey(q2, Set.of("1", "2"), "Vì A và B"))));
        IAccountLookup accounts = mock(IAccountLookup.class);
        when(accounts.getContact(any())).thenAnswer(call -> Optional.of(new AccountContact(call.getArgument(0),
                "sv@fpt.edu.vn", "Sinh viên", Role.STUDENT, AccountStatus.ACTIVE)));

        PracticeAccess access = new PracticeAccess(authorization, audits::add, clock);
        PracticeSupport support = new PracticeSupport(store, quizzes, scoring, classes, access, clock);
        attemptsService = new QuizAttemptService(store, support, access, clock);
        queries = new QuizPracticeQueryService(store, quizzes, classes, support, access);
        teacherService = new TeacherQuizAttemptService(store, classes, accounts, mock(IActivityQuery.class), support, access);
        scanner = new QuizAutoSubmitScanner(store, support);
        retired = new QuizRetiredHandler(store, support);
        activity = new QuizActivityService(activityWrite, support);
    }

    private ClassRef classRef(UUID id, ClassStatus status) {
        return new ClassRef(id, subject, "SE1801", "Lớp SE1801", status, teacher.accountId(), false);
    }

    private QuizView quiz() {
        return new QuizView(quizId, QuizScope.CLASS, classA, subject, 1, "Luyện tập", quizStatus, settings, monitoring,
                monitoring ? "Ghi lại khi rời trang" : null, List.of(), new BigDecimal("3"));
    }

    private static QuizQuestionView question(UUID id, QuestionType type) {
        return new QuizQuestionView(id, type, "Câu " + type, List.of(new QuizOptionView("1", "A"), new QuizOptionView("2", "B"),
                new QuizOptionView("3", "C")), type == QuestionType.MCQ_SINGLE ? BigDecimal.ONE : new BigDecimal("2"));
    }

    /** Chấm giả đúng như U10: đúng đủ tập đáp án mới có điểm. */
    private QuizScore score(List<QuizAnswer> answers) {
        Map<UUID, Set<String>> selected = new HashMap<>();
        answers.forEach(a -> selected.put(a.questionId(), a.selectedOptionIds()));
        boolean ok1 = Set.of("1").equals(selected.get(q1));
        boolean ok2 = Set.of("1", "2").equals(selected.get(q2));
        BigDecimal total = (ok1 ? BigDecimal.ONE : BigDecimal.ZERO).add(ok2 ? new BigDecimal("2") : BigDecimal.ZERO);
        return new QuizScore(List.of(new QuestionScore(q1, ok1, ok1 ? BigDecimal.ONE : BigDecimal.ZERO, BigDecimal.ONE),
                new QuestionScore(q2, ok2, ok2 ? new BigDecimal("2") : BigDecimal.ZERO, new BigDecimal("2"))), total,
                new BigDecimal("3"));
    }

    private static ApiException api(Runnable call) {
        try {
            call.run();
        } catch (ApiException failure) {
            return failure;
        }
        throw new AssertionError("Không ném ApiException");
    }

    @Test
    void startingCreatesOneAttemptAndReopensTheOneInProgress() {
        var first = attemptsService.start(student, quizId, classA);
        assertThat(first.attemptNo()).isEqualTo(1);
        assertThat(first.deadlineAt()).isEqualTo(clock.instant().plus(Duration.ofMinutes(10)));
        assertThat(first.questions()).extracting(q -> q.questionId()).containsExactly(q1, q2);
        assertThat(first.toString()).doesNotContain("Vì A");

        var again = attemptsService.start(student, quizId, classA);
        assertThat(again.attemptId()).isEqualTo(first.attemptId());
        assertThat(store.byId).hasSize(1);
    }

    @Test
    void attemptsAreLimitedAndUnlimitedWhenNoMax() {
        submitFresh(student);
        submitFresh(student);
        ApiException noMore = api(() -> attemptsService.start(student, quizId, classA));
        assertThat(noMore.code()).isEqualTo("NO_ATTEMPTS_LEFT");
        assertThat(audits).extracting(AuditEvent::action).contains("QUIZ_SUBMIT_REJECTED");
        assertThat(queries.detail(student, quizId, classA).blockedReason()).contains("2/2");

        settings = new QuizSettings(false, false, null, null, true, true);
        assertThat(attemptsService.start(student, quizId, classA).attemptNo()).isEqualTo(3);
    }

    @Test
    void onlyEnrolledStudentsOfTheQuizClassCanStart() {
        Actor stranger = new Actor(UUID.randomUUID(), Role.STUDENT);
        assertThat(api(() -> attemptsService.start(stranger, quizId, classA)).status()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(audits).extracting(AuditEvent::action).containsExactly("ACCESS_DENIED");
        assertThat(api(() -> attemptsService.start(teacher, quizId, classA)).status()).isEqualTo(HttpStatus.NOT_FOUND);
        quizStatus = QuizStatus.RETIRED;
        assertThat(api(() -> attemptsService.start(student, quizId, classA)).code()).isEqualTo("QUIZ_NOT_OPEN");
    }

    @Test
    void savingChecksVersionQuestionsAndDeadlineWithGrace() {
        var taking = attemptsService.start(student, quizId, classA);
        var saved = attemptsService.saveAnswers(student, taking.attemptId(), Map.of(q1, List.of("1")), 0);
        assertThat(saved.answerVersion()).isEqualTo(1);
        assertThat(api(() -> attemptsService.saveAnswers(student, taking.attemptId(), Map.of(q1, List.of("2")), 0)).code())
                .isEqualTo("ANSWER_VERSION_CONFLICT");
        assertThat(api(() -> attemptsService.saveAnswers(student, taking.attemptId(), Map.of(q1, List.of("9")), 1)).code())
                .isEqualTo("ANSWER_INVALID");
        assertThat(api(() -> attemptsService.saveAnswers(other, taking.attemptId(), Map.of(), 1)).status())
                .isEqualTo(HttpStatus.NOT_FOUND);

        clock.advance(Duration.ofMinutes(10).plusSeconds(20));
        assertThat(attemptsService.saveAnswers(student, taking.attemptId(), Map.of(q1, List.of("1"), q2, List.of("1", "2")), 1)
                .answerVersion()).as("trong 30 giây ân hạn").isEqualTo(2);
        clock.advance(Duration.ofSeconds(15));
        assertThat(api(() -> attemptsService.saveAnswers(student, taking.attemptId(), Map.of(), 2)).code())
                .isEqualTo("ATTEMPT_TIME_UP");
        assertThat(api(() -> attemptsService.submit(student, taking.attemptId())).code()).isEqualTo("ATTEMPT_TIME_UP");
    }

    @Test
    void submitScoresWithPinnedAnswersAndRepeatsReturnTheSameResult() {
        var taking = attemptsService.start(student, quizId, classA);
        attemptsService.saveAnswers(student, taking.attemptId(), Map.of(q1, List.of("1"), q2, List.of("1")), 0);

        var result = attemptsService.submit(student, taking.attemptId());

        assertThat(result.score()).isEqualByComparingTo("1");
        assertThat(result.maxScore()).isEqualByComparingTo("3");
        assertThat(result.correctCount()).isEqualTo(1);
        assertThat(result.wrongCount()).isEqualTo(1);
        assertThat(result.questions().get(1).correctOptionIds()).containsExactlyInAnyOrder("1", "2");
        assertThat(store.byId.get(taking.attemptId()).submitMode()).isEqualTo(SubmitMode.MANUAL);
        assertThat(attemptsService.submit(student, taking.attemptId()).score()).isEqualByComparingTo("1");
        assertThat(audits).extracting(AuditEvent::action).containsExactly("QUIZ_ATTEMPT_SUBMITTED");
        assertThat(audits.get(0).details().toString()).doesNotContain("[1]");
    }

    @Test
    void submitRescoresWhenAnAutosaveLandsBetweenScoringAndSubmit() {
        var taking = attemptsService.start(student, quizId, classA);
        attemptsService.saveAnswers(student, taking.attemptId(), Map.of(q1, List.of("2")), 0);
        store.beforeSubmit = () -> {
            store.beforeSubmit = null;
            attemptsService.saveAnswers(student, taking.attemptId(), Map.of(q1, List.of("1"), q2, List.of("1", "2")), 1);
        };

        var result = attemptsService.submit(student, taking.attemptId());

        QuizAttempt stored = store.byId.get(taking.attemptId());
        assertThat(stored.answerVersion()).isEqualTo(2);
        assertThat(stored.answers()).containsEntry(q1, List.of("1"));
        assertThat(stored.score()).as("điểm chấm trên câu trả lời đã lưu sau cùng").isEqualByComparingTo("3");
        assertThat(result.score()).isEqualByComparingTo("3");
        assertThat(result.correctCount()).isEqualTo(2);
        assertThat(audits).extracting(AuditEvent::action).containsExactly("QUIZ_ATTEMPT_SUBMITTED");
    }

    @Test
    void submitGivesUpWithConflictWhenSavesKeepLanding() {
        var taking = attemptsService.start(student, quizId, classA);
        long[] version = {0};
        store.beforeSubmit = () -> {
            attemptsService.saveAnswers(student, taking.attemptId(), Map.of(q1, List.of("1")), version[0]);
            version[0]++;
        };

        assertThat(api(() -> attemptsService.submit(student, taking.attemptId())).code())
                .isEqualTo("ANSWER_VERSION_CONFLICT");
        assertThat(store.byId.get(taking.attemptId()).inProgress()).isTrue();
        assertThat(version[0]).isEqualTo(PracticeSupport.SUBMIT_TRIES);
    }

    @Test
    void resultFollowsSnapshotSettingsAndIsOwnOnly() {
        settings = new QuizSettings(false, false, null, 3, false, false);
        UUID attemptId = submitFresh(student);
        var result = attemptsService.result(student, attemptId);
        assertThat(result.showScore()).isFalse();
        assertThat(result.score()).isNull();
        assertThat(result.questions()).allSatisfy(q -> {
            assertThat(q.correctOptionIds()).isNull();
            assertThat(q.explanation()).isNull();
            assertThat(q.points()).isNull();
        });
        assertThat(result.canRetry()).isTrue();
        assertThat(api(() -> attemptsService.result(other, attemptId)).status()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void scannerSubmitsOverdueAttemptsOnce() {
        var taking = attemptsService.start(student, quizId, classA);
        attemptsService.saveAnswers(student, taking.attemptId(), Map.of(q1, List.of("1")), 0);
        clock.advance(Duration.ofMinutes(10).plusSeconds(20));
        scanner.scan(clock.instant());
        assertThat(store.byId.get(taking.attemptId()).status()).as("chưa quá ân hạn").isEqualTo(AttemptStatus.IN_PROGRESS);

        clock.advance(Duration.ofSeconds(20));
        scanner.scan(clock.instant());
        scanner.scan(clock.instant());

        QuizAttempt done = store.byId.get(taking.attemptId());
        assertThat(done.status()).isEqualTo(AttemptStatus.SUBMITTED);
        assertThat(done.submitMode()).isEqualTo(SubmitMode.AUTO_TIME_LIMIT);
        assertThat(done.score()).isEqualByComparingTo("1");
        assertThat(audits).filteredOn(e -> e.action().equals("QUIZ_ATTEMPT_SUBMITTED")).hasSize(1);
    }

    @Test
    void retiredQuizSubmitsAttemptsInProgress() {
        var mine = attemptsService.start(student, quizId, classA);
        var theirs = attemptsService.start(other, quizId, classA);
        retired.onRetired(quizId);
        assertThat(store.byId.get(mine.attemptId()).submitMode()).isEqualTo(SubmitMode.AUTO_RETIRED);
        assertThat(store.byId.get(theirs.attemptId()).status()).isEqualTo(AttemptStatus.SUBMITTED);
    }

    @Test
    void historyShowsOpenQuizzesAndRetiredOnesWithOwnAttempts() {
        assertThat(queries.history(student, null)).singleElement().satisfies(row -> {
            assertThat(row.attemptsUsed()).isZero();
            assertThat(row.canStart()).isTrue();
        });
        submitFresh(student);
        quizStatus = QuizStatus.RETIRED;

        assertThat(queries.history(student, null)).singleElement().satisfies(row -> {
            assertThat(row.status()).isEqualTo(QuizStatus.RETIRED);
            assertThat(row.attemptsUsed()).isEqualTo(1);
            assertThat(row.bestScore()).isNotNull();
            assertThat(row.canStart()).isFalse();
        });
        assertThat(api(() -> queries.history(teacher, null)).status()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void materialListShowsLinkedOpenQuizzesWithOwnAttemptsForEnrolledClassOnly() {
        UUID material = UUID.randomUUID();
        when(quizzes.listByMaterial(material, classA)).thenAnswer(call -> List.of(quiz()));
        submitFresh(student);

        assertThat(queries.byMaterial(student, classA, material)).singleElement().satisfies(row -> {
            assertThat(row.quizId()).isEqualTo(quizId);
            assertThat(row.attemptsUsed()).isEqualTo(1);
        });
        assertThat(queries.byMaterial(student, classA, UUID.randomUUID())).isEmpty();
        assertThat(api(() -> queries.byMaterial(student, classB, material)).status()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(api(() -> queries.byMaterial(teacher, classA, material)).status()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void teacherSeesClassStudentsAttemptsReadOnly() {
        UUID attemptId = submitFresh(student);
        var list = teacherService.list(teacher, classA, quizId);
        assertThat(list.totalStudents()).isEqualTo(2);
        assertThat(list.studentsAttempted()).isEqualTo(1);
        assertThat(list.students().get(0).attempts()).isEqualTo(1);

        settings = new QuizSettings(false, false, null, 2, false, false);
        var detail = teacherService.detail(teacher, classA, attemptId);
        assertThat(detail.result().questions()).allSatisfy(q -> assertThat(q.correctOptionIds()).isNotNull());
        assertThat(api(() -> teacherService.detail(teacher, classB, attemptId)).status()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void activityNeedsMonitoringEnabledInSnapshot() {
        var taking = attemptsService.start(student, quizId, classA);
        assertThat(api(() -> activity.record(student, taking.attemptId(), List.of())).code())
                .isEqualTo("ACTIVITY_NOT_ENABLED");
        verify(activityWrite, never()).record(any(), any(), anyList());
        assertThat(taking.activityMonitoringEnabled()).isFalse();
    }

    @Test
    void takingViewCarriesSnapshotActivityPolicy() {
        monitoring = true;
        var taking = attemptsService.start(student, quizId, classA);
        monitoring = false;

        var resumed = attemptsService.get(student, taking.attemptId());
        assertThat(resumed.activityMonitoringEnabled()).as("theo ảnh chụp, không theo Quiz hiện tại").isTrue();
        assertThat(resumed.activityNotice()).isEqualTo("Ghi lại khi rời trang");
    }

    private UUID submitFresh(Actor actor) {
        var taking = attemptsService.start(actor, quizId, classA);
        attemptsService.saveAnswers(actor, taking.attemptId(), Map.of(q1, List.of("1")), taking.answerVersion());
        attemptsService.submit(actor, taking.attemptId());
        return taking.attemptId();
    }

    /** Đồng hồ chỉnh được để thử hết giờ và ân hạn. */
    static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    /** Repository giả: đủ cho luồng làm bài (không kiểm SQL). */
    static final class InMemoryAttempts implements IQuizAttemptRepository {
        final Map<UUID, QuizAttempt> byId = new LinkedHashMap<>();
        /** Chạy ngay trước câu lệnh nộp: giả một lần lưu từ request khác xen vào giữa lúc chấm và lúc nộp. */
        Runnable beforeSubmit;

        @Override
        public Optional<QuizAttempt> find(UUID id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public void lockStart(UUID quizId, UUID accountId) {
        }

        @Override
        public Optional<QuizAttempt> findInProgress(UUID quizId, UUID accountId) {
            return byId.values().stream().filter(a -> a.quizId().equals(quizId) && a.accountId().equals(accountId)
                    && a.inProgress()).findFirst();
        }

        @Override
        public int countAttempts(UUID quizId, UUID accountId) {
            return (int) byId.values().stream().filter(a -> a.quizId().equals(quizId) && a.accountId().equals(accountId)).count();
        }

        @Override
        public void insert(QuizAttempt attempt) {
            byId.put(attempt.id(), attempt);
        }

        @Override
        public boolean saveAnswers(QuizAttempt attempt, long expectedVersion) {
            QuizAttempt current = byId.get(attempt.id());
            if (current == null || !current.inProgress() || current.answerVersion() != expectedVersion) {
                return false;
            }
            byId.put(attempt.id(), attempt);
            return true;
        }

        @Override
        public boolean submit(QuizAttempt attempt) {
            if (beforeSubmit != null) {
                beforeSubmit.run();
            }
            QuizAttempt current = byId.get(attempt.id());
            if (current == null || !current.inProgress() || current.answerVersion() != attempt.answerVersion()) {
                return false;
            }
            byId.put(attempt.id(), attempt);
            return true;
        }

        @Override
        public List<QuizAttempt> listByAccount(UUID accountId, Collection<UUID> quizIds) {
            return byId.values().stream().filter(a -> a.accountId().equals(accountId) && quizIds.contains(a.quizId())).toList();
        }

        @Override
        public Map<UUID, UUID> quizzesAttemptedBy(UUID accountId) {
            Map<UUID, UUID> result = new LinkedHashMap<>();
            byId.values().stream().filter(a -> a.accountId().equals(accountId)).forEach(a -> result.put(a.quizId(), a.classId()));
            return result;
        }

        @Override
        public List<QuizAttempt> findOverdue(Instant cutoff, int limit) {
            return byId.values().stream().filter(a -> a.inProgress() && a.deadlineAt() != null
                    && a.deadlineAt().isBefore(cutoff)).limit(limit).toList();
        }

        @Override
        public List<QuizAttempt> findInProgressByQuiz(UUID quizId) {
            return byId.values().stream().filter(a -> a.quizId().equals(quizId) && a.inProgress()).toList();
        }

        @Override
        public List<QuizAttempt> listByQuizAndClass(UUID quizId, UUID classId) {
            return byId.values().stream().filter(a -> a.quizId().equals(quizId) && a.classId().equals(classId)).toList();
        }
    }
}
