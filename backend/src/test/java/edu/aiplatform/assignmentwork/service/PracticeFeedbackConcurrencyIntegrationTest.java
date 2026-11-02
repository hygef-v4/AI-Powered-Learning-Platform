package edu.aiplatform.assignmentwork.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;
import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.academics.contract.IClassAccess;
import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.ai.contract.AiSource;
import edu.aiplatform.ai.contract.AiTaskState;
import edu.aiplatform.ai.contract.AiTaskView;
import edu.aiplatform.ai.contract.IAiTask;
import edu.aiplatform.assignments.contract.AssignmentContent;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.GradingMode;
import edu.aiplatform.assignments.contract.IAssignmentQuery;
import edu.aiplatform.assignments.contract.IDocumentModel;
import edu.aiplatform.assignmentwork.contract.AttemptStatus;
import edu.aiplatform.assignmentwork.contract.SubmitMode;
import edu.aiplatform.assignmentwork.model.AssignmentAttempt;
import edu.aiplatform.assignmentwork.repository.AttemptRepository;
import edu.aiplatform.assignmentwork.model.AttemptSnapshot;
import edu.aiplatform.assignmentwork.repository.PracticeFeedbackRepository;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.AuthorizationDecision;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.shared.error.ApiException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Real PostgreSQL transactions and feedback SQL; AI/auth contracts are local stubs, with no provider calls. */
@Testcontainers
class PracticeFeedbackConcurrencyIntegrationTest {
    private static final Instant NOW = Instant.parse("2026-10-30T03:00:00Z");
    private static final String APP_PASSWORD = UUID.randomUUID().toString();
    private static final String MIGRATOR_PASSWORD = UUID.randomUUID().toString();
    private static final String APPLICATION_NAME = "feedback-concurrency-" + UUID.randomUUID();
    private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withEnv("POSTGRES_APP_PASSWORD", APP_PASSWORD)
            .withEnv("POSTGRES_MIGRATOR_PASSWORD", MIGRATOR_PASSWORD)
            .withCopyToContainer(rolesScript(), "/docker-entrypoint-initdb.d/01-roles.sh");

    private static HikariDataSource appSource;
    private static HikariDataSource ownerSource;
    private static JdbcClient owner;
    private static JdbcClient app;

    private final Actor student = new Actor(UUID.randomUUID(), Role.STUDENT);
    private final Actor other = new Actor(UUID.randomUUID(), Role.STUDENT);
    private final UUID subjectId = UUID.randomUUID();
    private final UUID classId = UUID.randomUUID();
    private final UUID assignmentId = UUID.randomUUID();
    private final DocumentModel content = new DocumentModel(List.of());
    private AttemptRepository attempts;
    private CountGateFeedback feedback;
    private PracticeFeedbackService service;
    private AssignmentAttempt first;
    private AssignmentAttempt second;
    private AssignmentAttempt otherAttempt;

    @BeforeAll
    static void migrate() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), "migrator", MIGRATOR_PASSWORD)
                .locations("classpath:db/migration").load().migrate();
        appSource = source("app", APP_PASSWORD);
        appSource.addDataSourceProperty("ApplicationName", APPLICATION_NAME);
        ownerSource = source(POSTGRES.getUsername(), POSTGRES.getPassword());
        owner = JdbcClient.create(ownerSource);
        app = JdbcClient.create(appSource);
    }

    @AfterAll
    static void close() {
        if (appSource != null) appSource.close();
        if (ownerSource != null) ownerSource.close();
    }

    @BeforeEach
    void seed() {
        owner.sql("truncate accounts, subjects cascade").update();
        UUID teacher = UUID.randomUUID();
        for (Actor actor : List.of(student, other, new Actor(teacher, Role.TEACHER))) {
            owner.sql("insert into accounts (id, school_email, role, status, display_name, password_hash) "
                            + "values (?, ?, ?, 'ACTIVE', 'Fixture', 'fixture-hash')")
                    .params(actor.accountId(), actor.accountId() + "@example.test", actor.role().name()).update();
        }
        owner.sql("insert into subjects (id, code, name, created_at, updated_at) values (?, 'TEST', 'Practice', now(), now())")
                .params(subjectId).update();
        owner.sql("insert into course_classes (id, subject_id, code, name, status, teacher_id, created_at, updated_at) "
                        + "values (?, ?, 'C1', 'Class', 'OPEN', ?, now(), now())")
                .params(classId, subjectId, teacher).update();
        for (Actor actor : List.of(student, other)) {
            owner.sql("insert into enrollments (class_id, account_id, status, source, enrolled_at) "
                            + "values (?, ?, 'ACTIVE', 'MANUAL', now())")
                    .params(classId, actor.accountId()).update();
        }
        owner.sql("insert into assignments (id, scope, class_id, subject_id, origin, submission_mode, grading_mode, title, "
                        + "skeleton, parts, status, opens_at, closes_at, max_attempts, created_by, created_at, updated_at) "
                        + "values (?, 'CLASS', ?, ?, 'MANUAL', 'INDIVIDUAL', 'PRACTICE', 'Practice', "
                        + "'{\"blocks\":[]}'::jsonb, '[]'::jsonb, 'OPEN', now() - interval '1 hour', now() + interval '1 hour', 3, ?, now(), now())")
                .params(assignmentId, classId, subjectId, teacher).update();
        attempts = new AttemptRepository(app, JSON);
        first = attempt(student, 1);
        second = attempt(student, 2);
        otherAttempt = attempt(other, 1);
        feedback = new CountGateFeedback(app, JSON);
        service = service(1);
    }

    private PracticeFeedbackService service(int perDay) {
        IAuthorization authorization = (actor, action, ref) -> AuthorizationDecision.allow();
        IClassAccess classes = mock(IClassAccess.class);
        when(classes.getClassRef(classId)).thenReturn(Optional.of(
                new ClassRef(classId, subjectId, "C1", "Class", ClassStatus.OPEN, null, false)));
        IAssignmentQuery assignments = mock(IAssignmentQuery.class);
        when(assignments.getContent(assignmentId)).thenReturn(Optional.of(
                new AssignmentContent(assignmentId, 1, "Instructions", content, List.of(), Map.of(), List.of())));
        IDocumentModel documents = mock(IDocumentModel.class);
        when(documents.toPlainText(any())).thenReturn("Practice text");
        IAiTask ai = mock(IAiTask.class);
        when(ai.submit(any())).thenAnswer(call -> new AiTaskView(UUID.randomUUID(), AiTaskState.QUEUED,
                AiSource.PERSONAL, AiProvider.GEMINI, "fixture-model", null, null, null));
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        WorkAudit audit = new WorkAudit(mock(IAudit.class), clock);
        WorkAccess access = new WorkAccess(authorization, classes, assignments, attempts, audit);
        return new PracticeFeedbackService(access, attempts, feedback, assignments, documents, ai, audit, JSON,
                new DataSourceTransactionManager(appSource), clock, perDay);
    }

    private AssignmentAttempt attempt(Actor actor, int number) {
        AssignmentAttempt attempt = new AssignmentAttempt(UUID.randomUUID(), assignmentId, classId, actor.accountId(), number,
                new AttemptSnapshot(1, "Practice", GradingMode.PRACTICE, NOW.plusSeconds(3600), null,
                        Map.of(), false, false, null, 3), content, 0, AttemptStatus.SUBMITTED, SubmitMode.MANUAL,
                false, NOW.minusSeconds(600), NOW.plusSeconds(3600), NOW.minusSeconds(60), NOW.minusSeconds(30),
                "submission-" + actor.accountId() + "-" + number, List.of());
        attempts.insert(attempt);
        return attempt;
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void apiAndMcpConcurrentRequestsForDifferentAttemptsShareOneDailySlot(boolean mcpFirst) throws Exception {
        List<String> outcomes = race(() -> invoke(student, first, mcpFirst), () -> invoke(student, second, !mcpFirst), false);

        assertThat(outcomes).containsExactly("ACCEPTED", "FEEDBACK_LIMIT");
        assertThat(feedback.countSince(student.accountId(), NOW.minusSeconds(1))).isEqualTo(1);
    }

    @Test
    void incomingMcpCannotPassPendingCheckBeforeConcurrentApiReservationCommits() throws Exception {
        service = service(20);

        List<String> outcomes = race(() -> invoke(student, first, false), () -> invoke(student, first, true), false);

        assertThat(outcomes).containsExactly("ACCEPTED", "FEEDBACK_PENDING");
        assertThat(feedback.listByAttempt(first.id())).singleElement()
                .satisfies(row -> assertThat(row.status().name()).isEqualTo("PENDING"));
    }

    @Test
    void separateAccountsCanReserveIndependentlyWhileFirstAccountIsPaused() throws Exception {
        List<String> outcomes = race(() -> invoke(student, first, false), () -> invoke(other, otherAttempt, true), true);

        assertThat(outcomes).containsExactly("ACCEPTED", "ACCEPTED");
        assertThat(feedback.countSince(student.accountId(), NOW.minusSeconds(1))).isEqualTo(1);
        assertThat(feedback.countSince(other.accountId(), NOW.minusSeconds(1))).isEqualTo(1);
    }

    @Test
    void sequentialMcpReservationConsumesTheApiDailySlot() {
        feedback.pauseFirstCount.set(false);
        assertThat(invoke(student, first, true)).isEqualTo("ACCEPTED");
        assertThatThrownBy(() -> service.request(student, second.id(), AiProvider.GEMINI, UUID.randomUUID().toString()))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("FEEDBACK_LIMIT"));
        assertThat(feedback.countSince(student.accountId(), NOW.minusSeconds(1))).isEqualTo(1);
    }

    private String invoke(Actor actor, AssignmentAttempt attempt, boolean mcp) {
        try {
            if (mcp) {
                service.recordFromAgent(actor, attempt.id(), attempt.receiptHash(), JSON.valueToTree(Map.of(
                        "summary", "Useful", "parts", List.of(), "suggestions", List.of("Example"))), "fixture agent");
            } else {
                service.request(actor, attempt.id(), AiProvider.GEMINI, UUID.randomUUID().toString());
            }
            return "ACCEPTED";
        } catch (ApiException rejected) {
            return rejected.code();
        }
    }

    /** Pause after a real quota read, then let a competing transaction finish or wait on a PostgreSQL lock. */
    private List<String> race(Callable<String> firstRequest, Callable<String> secondRequest,
                              boolean independentAccounts) throws Exception {
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<String> firstResult = workers.submit(firstRequest);
            assertThat(feedback.countRead.await(10, TimeUnit.SECONDS)).as("first transaction read daily count").isTrue();
            Future<String> secondResult = workers.submit(secondRequest);
            long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
            boolean waiting = false;
            while (!secondResult.isDone() && System.nanoTime() < deadline) {
                waiting = owner.sql("select exists (select 1 from pg_stat_activity where application_name = :name "
                                + "and wait_event_type = 'Lock')")
                        .param("name", APPLICATION_NAME).query(Boolean.class).single();
                if (waiting) break;
                LockSupport.parkNanos(Duration.ofMillis(10).toNanos());
            }
            assertThat(secondResult.isDone() || waiting).as("competing transaction completed or reached database lock").isTrue();
            if (independentAccounts) {
                assertThat(secondResult.isDone()).as("different account completes before first reservation is released").isTrue();
            }
            feedback.releaseCount.countDown();
            return List.of(firstResult.get(15, TimeUnit.SECONDS), secondResult.get(15, TimeUnit.SECONDS));
        } finally {
            feedback.releaseCount.countDown();
            workers.shutdownNow();
            assertThat(workers.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private static final class CountGateFeedback extends PracticeFeedbackRepository {
        final AtomicBoolean pauseFirstCount = new AtomicBoolean(true);
        final CountDownLatch countRead = new CountDownLatch(1);
        final CountDownLatch releaseCount = new CountDownLatch(1);

        CountGateFeedback(JdbcClient jdbc, ObjectMapper json) { super(jdbc, json); }

        @Override public long countSince(UUID accountId, Instant since) {
            long count = super.countSince(accountId, since);
            if (pauseFirstCount.compareAndSet(true, false)) {
                countRead.countDown();
                try {
                    if (!releaseCount.await(30, TimeUnit.SECONDS)) throw new AssertionError("quota read gate timed out");
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("quota read interrupted", interrupted);
                }
            }
            return count;
        }
    }

    private static HikariDataSource source(String user, String password) {
        HikariDataSource source = new HikariDataSource();
        source.setJdbcUrl(POSTGRES.getJdbcUrl());
        source.setUsername(user);
        source.setPassword(password);
        source.setMaximumPoolSize(4);
        return source;
    }

    private static Transferable rolesScript() {
        try {
            return Transferable.of(Files.readString(Path.of("..", "infra", "postgres", "init", "01-roles.sh"))
                    .replace("\r", ""), 0755);
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
    }
}
