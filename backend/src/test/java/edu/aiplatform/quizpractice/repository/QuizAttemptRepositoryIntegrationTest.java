package edu.aiplatform.quizpractice.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;
import edu.aiplatform.quizpractice.model.ActivityOutcome;
import edu.aiplatform.quizpractice.model.ActivitySignal;
import edu.aiplatform.quizpractice.model.AttemptSnapshot;
import edu.aiplatform.quizpractice.model.AttemptStatus;
import edu.aiplatform.quizpractice.model.PenaltyDecision;
import edu.aiplatform.quizpractice.model.QuestionResult;
import edu.aiplatform.quizpractice.model.QuizAttempt;
import edu.aiplatform.quizpractice.model.SubmitMode;
import edu.aiplatform.quizzes.contract.QuizSettings;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.time.Clock;
import edu.aiplatform.academics.contract.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.assignmentwork.contract.IActivityQuery;
import edu.aiplatform.subjects.contract.ISubjectQuery;
import edu.aiplatform.quizzes.contract.*;
import edu.aiplatform.quizzes.model.*;
import edu.aiplatform.quizzes.repository.*;
import edu.aiplatform.quizzes.service.QuizAuthoringService;
import edu.aiplatform.quizzes.service.QuizScorer;
import edu.aiplatform.quizpractice.service.QuizAttemptService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import static org.mockito.Mockito.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** SQL viết tay của {@link QuizAttemptRepository} và migration U11 trên PostgreSQL 16. */
@Testcontainers(disabledWithoutDocker = true)
class QuizAttemptRepositoryIntegrationTest {

    // Sinh mỗi lần chạy: container chỉ sống trong test, không để chuỗi mật khẩu cố định trong mã.
    private static final String APP_PASSWORD = UUID.randomUUID().toString();
    private static final String MIGRATOR_PASSWORD = UUID.randomUUID().toString();
    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.MICROS);

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withEnv("POSTGRES_APP_PASSWORD", APP_PASSWORD)
            .withEnv("POSTGRES_MIGRATOR_PASSWORD", MIGRATOR_PASSWORD)
            .withCopyToContainer(rolesScript(), "/docker-entrypoint-initdb.d/01-roles.sh");

    private static HikariDataSource appDataSource;
    private static HikariDataSource adminDataSource;
    private static JdbcClient admin;
    private static QuizAttemptRepository repository;
    private static TransactionTemplate tx;

    private UUID student;
    private UUID classId;
    private UUID quizId;

    @BeforeAll
    static void migrate() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), "migrator", MIGRATOR_PASSWORD)
                .locations("classpath:db/migration").load().migrate();
        appDataSource = dataSource("app", APP_PASSWORD);
        adminDataSource = dataSource(POSTGRES.getUsername(), POSTGRES.getPassword());
        admin = JdbcClient.create(adminDataSource);
        repository = new QuizAttemptRepository(JdbcClient.create(appDataSource), new ObjectMapper());
        tx = new TransactionTemplate(new DataSourceTransactionManager(appDataSource));
    }

    @AfterAll
    static void close() {
        if (appDataSource != null) {
            appDataSource.close();
        }
        if (adminDataSource != null) {
            adminDataSource.close();
        }
    }

    @BeforeEach
    void seed() {
        admin.sql("truncate quiz_attempts, quiz_items, questions, quiz_ai_drafts, quizzes, course_classes, subjects,"
                + " accounts cascade").update();
        student = UUID.randomUUID();
        UUID subject = UUID.randomUUID();
        classId = UUID.randomUUID();
        quizId = UUID.randomUUID();
        admin.sql("insert into accounts (id, school_email, role, status, display_name) values (:id, :email, 'STUDENT',"
                + " 'PENDING', 'Sinh viên')").param("id", student).param("email", student + "@fpt.edu.vn").update();
        admin.sql("insert into subjects (id, code, name, status, created_at, updated_at) values (:id, 'SWT301',"
                + " 'Kiểm thử', 'ACTIVE', now(), now())").param("id", subject).update();
        admin.sql("insert into course_classes (id, subject_id, code, name, status, starts_on, created_at, updated_at)"
                + " values (:id, :subject, 'SE1801', 'Lớp', 'DRAFT', DATE '2026-11-14', now(), now())")
                .param("id", classId).param("subject", subject).update();
        admin.sql("insert into quizzes (id, scope, class_id, subject_id, title, settings, created_by, created_at,"
                        + " updated_at) values (:id, 'CLASS', :classId, :subject, 'Quiz', '{}'::jsonb, :author, now() - interval '1 day', now() - interval '1 day')")
                .param("id", quizId).param("classId", classId).param("subject", subject).param("author", student).update();
    }

    private static Transferable rolesScript() {
        try {
            String script = Files.readString(Path.of("..", "infra", "postgres", "init", "01-roles.sh"));
            return Transferable.of(script.replace("\r", ""), 0755);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static HikariDataSource dataSource(String user, String password) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(POSTGRES.getJdbcUrl());
        dataSource.setUsername(user);
        dataSource.setPassword(password);
        dataSource.setMaximumPoolSize(4);
        return dataSource;
    }

    private QuizAttempt attempt(int no, Integer minutes) {
        UUID q = UUID.randomUUID();
        AttemptSnapshot snapshot = new AttemptSnapshot(1, "Quiz", new QuizSettings(true, false, minutes, 2, true, false),
                false, null, 99L, List.of(q), Map.of(q, List.of("2", "1")), new BigDecimal("2"));
        return QuizAttempt.start(quizId, classId, student, no, snapshot, NOW);
    }

    @Test
    void firstAttemptWaitsForQuestionEditAndUsesItsCommittedPaperAndKey() throws Exception {
        JdbcClient app = JdbcClient.create(appDataSource);
        ObjectMapper json = new ObjectMapper().findAndRegisterModules();
        var quizzes = new QuizRepository(app, json);
        var questions = new QuestionRepository(app, json);
        var quiz = quizzes.findQuiz(quizId).orElseThrow();
        questions.insert(Question.createInline(quizId, quiz.subjectId(), createQuestionContent("Old paper", "1"), student, NOW));
        UUID questionId = questions.listInline(quizId).get(0).id();
        quizzes.replaceItems(quizId, List.of(new QuizItem(quizId, questionId, 1, BigDecimal.ONE)));
        assertThat(quizzes.updateQuiz(quiz.markPublished(Instant.now()))).isTrue();
        long version = quizzes.findQuiz(quizId).orElseThrow().version();
        CountDownLatch checkedPresence = new CountDownLatch(1);
        CountDownLatch resumeEdit = new CountDownLatch(1);
        CountDownLatch startedTransaction = new CountDownLatch(1);
        IClassAccess classes = mock(IClassAccess.class);
        when(classes.getClassRef(classId)).thenReturn(Optional.of(new ClassRef(classId, quiz.subjectId(),
                "SE1801", "Class", ClassStatus.OPEN, student, false)));
        ISubjectQuery subjects = mock(ISubjectQuery.class);
        when(subjects.isActive(quiz.subjectId())).thenReturn(true);
        var executor = Executors.newFixedThreadPool(2);
        try (var context = new AnnotationConfigApplicationContext()) {
            context.registerBean(IQuizRepository.class, () -> quizzes);
            context.registerBean(IQuestionRepository.class, () -> questions);
            context.registerBean(IQuizAttemptRepository.class, () -> repository);
            context.registerBean(IClassAccess.class, () -> classes);
            context.registerBean(ISubjectQuery.class, () -> subjects);
            context.registerBean(IAuthorization.class, () -> (actor, action, ref) -> AuthorizationDecision.allow());
            context.registerBean(IAudit.class, () -> event -> { });
            context.registerBean(IActivityQuery.class, () -> mock(IActivityQuery.class));
            context.registerBean(Clock.class, Clock::systemUTC);
            context.registerBean(IQuizAttemptPresence.class, () -> id -> {
                boolean present = repository.existsForQuiz(id);
                checkedPresence.countDown();
                awaitLatch(resumeEdit);
                return present;
            });
            for (String name : List.of("quizzes.service.QuizAuthoringService", "quizzes.service.QuizSupport",
                    "quizzes.service.QuizAccess", "quizzes.service.ActiveSubjects",
                    "quizzes.service.QuizQueryService", "quizzes.service.QuizPaperService", "quizzes.service.QuizScorer",
                    "quizpractice.service.QuizAttemptService", "quizpractice.service.PracticeSupport",
                    "quizpractice.service.PracticeAccess", "quizpractice.service.AttemptActivityAssessor")) {
                context.register(Class.forName("edu.aiplatform." + name));
            }
            context.refresh();
            Actor actor = new Actor(student, Role.STUDENT);
            var edit = executor.submit(() -> tx.execute(status -> context.getBean(QuizAuthoringService.class)
                    .updateInline(actor, quizId, questionId, createQuestionContent("Updated paper", "2"), version)));
            try {
                assertThat(checkedPresence.await(5, TimeUnit.SECONDS)).isTrue();
                var start = executor.submit(() -> tx.execute(status -> {
                    startedTransaction.countDown();
                    return context.getBean(QuizAttemptService.class).start(actor, quizId, classId);
                }));
                assertThat(startedTransaction.await(5, TimeUnit.SECONDS)).isTrue();
                try {
                    start.get(500, TimeUnit.MILLISECONDS);
                } catch (TimeoutException expectedWhileEditing) {
                    // The attempt waits until the edit commits its paper and key.
                }
                resumeEdit.countDown();
                edit.get(5, TimeUnit.SECONDS);
                var taking = start.get(5, TimeUnit.SECONDS);
                assertThat(taking.questions()).singleElement().satisfies(question ->
                        assertThat(question.stem()).isEqualTo("Updated paper"));
                assertThat(context.getBean(QuizScorer.class).score(quizId,
                        List.of(new QuizAnswer(questionId, Set.of("2")))).total()).isEqualByComparingTo("1");
                assertThatThrownBy(() -> tx.execute(status -> context.getBean(QuizAuthoringService.class)
                        .updateInline(actor, quizId, questionId, createQuestionContent("Later paper", "1"), version + 1)))
                        .isInstanceOf(edu.aiplatform.shared.error.ApiException.class)
                        .extracting(error -> ((edu.aiplatform.shared.error.ApiException) error).readCode())
                        .isEqualTo("QUIZ_HAS_ATTEMPTS");
            } finally {
                resumeEdit.countDown();
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private static QuestionContent createQuestionContent(String stem, String correctOption) {
        return QuestionContent.createContent(QuestionType.MCQ_SINGLE, stem,
                List.of(new QuestionOption("1", "First"), new QuestionOption("2", "Second")),
                List.of(correctOption), null, BigDecimal.ONE, Difficulty.MEDIUM, List.of());
    }

    private static void awaitLatch(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for edit release");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(interrupted);
        }
    }

    @Test
    void differentStudentsCanLockTheSameQuizWhileSameStudentAndEditsWait() throws Exception {
        var acquired = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var workers = Executors.newFixedThreadPool(3);
        try {
            var first = workers.submit(() -> tx.executeWithoutResult(status -> {
                repository.lockStart(quizId, student);
                acquired.countDown();
                awaitLatch(release);
            }));
            assertThat(acquired.await(5, TimeUnit.SECONDS)).isTrue();
            var otherStudent = workers.submit(() -> tx.executeWithoutResult(status ->
                    repository.lockStart(quizId, UUID.randomUUID())));
            otherStudent.get(2, TimeUnit.SECONDS);
            var sameStudent = workers.submit(() -> tx.executeWithoutResult(status ->
                    repository.lockStart(quizId, student)));
            var edit = workers.submit(() -> tx.executeWithoutResult(status ->
                    JdbcClient.create(appDataSource).sql("select id from quizzes where id = :id for update")
                            .param("id", quizId).query(UUID.class).single()));
            assertThatThrownBy(() -> sameStudent.get(200, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            assertThatThrownBy(() -> edit.get(200, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            release.countDown();
            first.get(5, TimeUnit.SECONDS);
            sameStudent.get(5, TimeUnit.SECONDS);
            edit.get(5, TimeUnit.SECONDS);
        } finally {
            release.countDown();
            workers.shutdownNow();
        }
    }

    @Test
    void attemptRoundTripsWithSnapshotAnswersAndResult() {
        QuizAttempt attempt = attempt(1, 15);
        tx.executeWithoutResult(s -> {
            repository.lockStart(quizId, student);
            repository.insert(attempt);
        });
        UUID q = attempt.snapshot().questionOrder().get(0);

        QuizAttempt saved = attempt.replaceAnswers(Map.of(q, List.of("2")), NOW.plusSeconds(5));
        assertThat(repository.saveAnswers(saved, 0)).isTrue();
        assertThat(repository.saveAnswers(saved, 0)).as("answerVersion cũ").isFalse();
        QuizAttempt stale = attempt.markSubmitted(SubmitMode.MANUAL, BigDecimal.ZERO, new BigDecimal("2"),
                List.of(new QuestionResult(q, false, BigDecimal.ZERO, new BigDecimal("2"))), NOW.plusSeconds(7));
        assertThat(repository.submit(stale)).as("chấm trên answerVersion cũ").isFalse();

        QuizAttempt submitted = saved.markSubmitted(SubmitMode.MANUAL, new BigDecimal("2"), new BigDecimal("2"),
                List.of(new QuestionResult(q, true, new BigDecimal("2"), new BigDecimal("2"))), NOW.plusSeconds(9));
        assertThat(repository.submit(submitted)).isTrue();
        assertThat(repository.submit(submitted)).as("nộp lần hai").isFalse();

        QuizAttempt read = repository.findAttempt(attempt.id()).orElseThrow();
        assertThat(read.status()).isEqualTo(AttemptStatus.SUBMITTED);
        assertThat(read.snapshot()).isEqualTo(attempt.snapshot());
        assertThat(read.answers()).isEqualTo(Map.of(q, List.of("2")));
        assertThat(read.answerVersion()).isEqualTo(1);
        assertThat(read.result()).singleElement().satisfies(r -> assertThat(r.correct()).isTrue());
        assertThat(read.correctCount()).isEqualTo(1);
        assertThat(repository.countAttempts(quizId, student)).isEqualTo(1);
        assertThat(repository.mapQuizzesAttemptedBy(student)).containsEntry(quizId, classId);
        assertThat(repository.listByQuizAndClass(quizId, classId)).hasSize(1);
    }

    @Test
    void newClassHasIndependentAttemptNumberAndInProgressSlot() {
        QuizAttempt old = attempt(1, null);
        repository.insert(old);
        UUID newClass = UUID.randomUUID();
        admin.sql("insert into course_classes (id, subject_id, code, name, status, starts_on, created_at, updated_at)"
                + " select :id, subject_id, 'RETAKE', 'Retake', 'OPEN', starts_on, now(), now()"
                + " from course_classes where id = :old")
                .param("id", newClass).param("old", classId).update();
        QuizAttempt fresh = QuizAttempt.start(quizId, newClass, student, 1, old.snapshot(), NOW);
        repository.insert(fresh);
        assertThat(repository.findInProgress(quizId, student, newClass)).contains(fresh);
        assertThat(repository.countAttempts(quizId, student, newClass)).isEqualTo(1);
        assertThat(repository.findAttempt(old.id())).contains(old);
        assertThatThrownBy(() -> repository.insert(
                QuizAttempt.start(quizId, newClass, student, 2, old.snapshot(), NOW)))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void penaltyDecisionChangesScoreAndCanBeRevised() {
        QuizAttempt attempt = attempt(1, null);
        repository.insert(attempt);
        UUID q = attempt.snapshot().questionOrder().get(0);
        BigDecimal raw = new BigDecimal("2");
        QuizAttempt submitted = attempt.markSubmitted(SubmitMode.MANUAL, raw, raw,
                List.of(new QuestionResult(q, true, raw, raw)), NOW.plusSeconds(9));
        assertThat(repository.submit(submitted.applyOutcome(new ActivityOutcome(raw, new BigDecimal("1.50"), BigDecimal.ZERO, 3,
                ActivitySignal.OK, null, null, null, null), raw))).isTrue();

        QuizAttempt pending = repository.findAttempt(attempt.id()).orElseThrow();
        assertThat(pending.score()).isEqualByComparingTo("2");
        assertThat(pending.outcome().suggestedPenalty()).isEqualByComparingTo("1.5");
        assertThat(pending.outcome().awaitsDecision()).isTrue();

        assertThat(repository.decidePenalty(attempt.id(), PenaltyDecision.APPLIED, new BigDecimal("3"), student, NOW, "Quá điểm gốc"))
                .as("không trừ quá điểm gốc").isFalse();
        assertThat(repository.decidePenalty(attempt.id(), PenaltyDecision.APPLIED, new BigDecimal("1"), student, NOW, "Rời nhiều lần")).isTrue();
        QuizAttempt applied = repository.findAttempt(attempt.id()).orElseThrow();
        assertThat(applied.score()).isEqualByComparingTo("1");
        assertThat(applied.outcome().penaltyPoints()).isEqualByComparingTo("1");
        assertThat(applied.outcome().decision()).isEqualTo(PenaltyDecision.APPLIED);
        assertThat(applied.outcome().decidedBy()).isEqualTo(student);
        assertThat(applied.outcome().decisionReason()).isEqualTo("Rời nhiều lần");

        assertThat(repository.decidePenalty(attempt.id(), PenaltyDecision.DISMISSED, BigDecimal.ZERO, student, NOW, "Báo nhầm")).isTrue();
        QuizAttempt dismissed = repository.findAttempt(attempt.id()).orElseThrow();
        assertThat(dismissed.score()).as("đổi quyết định tính lại từ điểm gốc").isEqualByComparingTo("2");
        assertThat(dismissed.outcome().decision()).isEqualTo(PenaltyDecision.DISMISSED);
    }

    @Test
    void penaltyDecisionNeedsASubmittedAttemptWithSuggestion() {
        QuizAttempt attempt = attempt(1, null);
        repository.insert(attempt);
        assertThat(repository.decidePenalty(attempt.id(), PenaltyDecision.APPLIED, BigDecimal.ONE, student, NOW, "Chưa nộp")).isFalse();
    }

    @Test
    void onlyOneAttemptInProgressPerQuizAndStudent() {
        repository.insert(attempt(1, null));
        assertThat(repository.findInProgress(quizId, student)).isPresent();
        assertThatThrownBy(() -> repository.insert(attempt(2, null))).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void overdueAttemptsAreFoundForTheScanner() {
        QuizAttempt timed = attempt(1, 1);
        repository.insert(timed);
        List<QuizAttempt> none = tx.execute(s -> repository.findOverdue(NOW, 10));
        assertThat(none).isEmpty();
        List<QuizAttempt> overdue = tx.execute(s -> repository.findOverdue(NOW.plusSeconds(61), 10));
        assertThat(overdue).extracting(QuizAttempt::id).containsExactly(timed.id());
        List<QuizAttempt> inProgress = tx.execute(s -> repository.findInProgressByQuiz(quizId));
        assertThat(inProgress).hasSize(1);
        assertThat(repository.listByAccount(student, List.of(quizId))).hasSize(1);
        assertThat(repository.listByAccount(student, List.of())).isEmpty();
    }

    @Test
    void inProgressAttemptsAreListedByClassAndCanBeSubmittedAsAccessLost() {
        QuizAttempt attempt = attempt(1, null);
        repository.insert(attempt);
        assertThat(repository.listClassesWithInProgress()).containsExactly(classId);
        assertThat(repository.listInProgressByClass(classId)).extracting(QuizAttempt::id).containsExactly(attempt.id());

        UUID q = attempt.snapshot().questionOrder().get(0);
        QuizAttempt submitted = attempt.markSubmitted(SubmitMode.AUTO_ACCESS_LOST, BigDecimal.ZERO, new BigDecimal("2"),
                List.of(new QuestionResult(q, false, BigDecimal.ZERO, new BigDecimal("2"))), NOW.plusSeconds(9));
        assertThat(repository.submit(submitted)).as("check constraint nhận chế độ mới").isTrue();

        assertThat(repository.findAttempt(attempt.id()).orElseThrow().submitMode()).isEqualTo(SubmitMode.AUTO_ACCESS_LOST);
        assertThat(repository.listClassesWithInProgress()).isEmpty();
        assertThat(repository.listInProgressByClass(classId)).isEmpty();
    }

    @Test
    void appCannotDeleteAttempts() {
        repository.insert(attempt(1, null));
        JdbcClient app = JdbcClient.create(appDataSource);
        assertThatThrownBy(() -> app.sql("delete from quiz_attempts").update())
                .hasRootCauseMessage("ERROR: permission denied for table quiz_attempts");
    }
}
