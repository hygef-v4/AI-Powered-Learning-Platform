package edu.aiplatform.quizpractice.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;
import edu.aiplatform.quizpractice.model.AttemptSnapshot;
import edu.aiplatform.quizpractice.model.AttemptStatus;
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

/** SQL viết tay của {@link JdbcQuizAttemptRepository} và migration U11 trên PostgreSQL 16. */
@Testcontainers(disabledWithoutDocker = true)
class JdbcQuizAttemptRepositoryIntegrationTest {

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
    private static JdbcQuizAttemptRepository repository;
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
        repository = new JdbcQuizAttemptRepository(JdbcClient.create(appDataSource), new ObjectMapper());
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
        admin.sql("insert into course_classes (id, subject_id, code, name, status, created_at, updated_at)"
                + " values (:id, :subject, 'SE1801', 'Lớp', 'DRAFT', now(), now())")
                .param("id", classId).param("subject", subject).update();
        admin.sql("insert into quizzes (id, scope, class_id, subject_id, title, settings, created_by, created_at,"
                        + " updated_at) values (:id, 'CLASS', :classId, :subject, 'Quiz', '{}'::jsonb, :author, now(), now())")
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
    void attemptRoundTripsWithSnapshotAnswersAndResult() {
        QuizAttempt attempt = attempt(1, 15);
        tx.executeWithoutResult(s -> {
            repository.lockStart(quizId, student);
            repository.insert(attempt);
        });
        UUID q = attempt.snapshot().questionOrder().get(0);

        QuizAttempt saved = attempt.withAnswers(Map.of(q, List.of("2")), NOW.plusSeconds(5));
        assertThat(repository.saveAnswers(saved, 0)).isTrue();
        assertThat(repository.saveAnswers(saved, 0)).as("answerVersion cũ").isFalse();
        QuizAttempt stale = attempt.submitted(SubmitMode.MANUAL, BigDecimal.ZERO, new BigDecimal("2"),
                List.of(new QuestionResult(q, false, BigDecimal.ZERO, new BigDecimal("2"))), NOW.plusSeconds(7));
        assertThat(repository.submit(stale)).as("chấm trên answerVersion cũ").isFalse();

        QuizAttempt submitted = saved.submitted(SubmitMode.MANUAL, new BigDecimal("2"), new BigDecimal("2"),
                List.of(new QuestionResult(q, true, new BigDecimal("2"), new BigDecimal("2"))), NOW.plusSeconds(9));
        assertThat(repository.submit(submitted)).isTrue();
        assertThat(repository.submit(submitted)).as("nộp lần hai").isFalse();

        QuizAttempt read = repository.find(attempt.id()).orElseThrow();
        assertThat(read.status()).isEqualTo(AttemptStatus.SUBMITTED);
        assertThat(read.snapshot()).isEqualTo(attempt.snapshot());
        assertThat(read.answers()).isEqualTo(Map.of(q, List.of("2")));
        assertThat(read.answerVersion()).isEqualTo(1);
        assertThat(read.result()).singleElement().satisfies(r -> assertThat(r.correct()).isTrue());
        assertThat(read.correctCount()).isEqualTo(1);
        assertThat(repository.countAttempts(quizId, student)).isEqualTo(1);
        assertThat(repository.quizzesAttemptedBy(student)).containsEntry(quizId, classId);
        assertThat(repository.listByQuizAndClass(quizId, classId)).hasSize(1);
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
    void appCannotDeleteAttempts() {
        repository.insert(attempt(1, null));
        JdbcClient app = JdbcClient.create(appDataSource);
        assertThatThrownBy(() -> app.sql("delete from quiz_attempts").update())
                .hasRootCauseMessage("ERROR: permission denied for table quiz_attempts");
    }
}
