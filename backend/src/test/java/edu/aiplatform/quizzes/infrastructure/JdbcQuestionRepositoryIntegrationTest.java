package edu.aiplatform.quizzes.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;
import edu.aiplatform.quizzes.contract.QuestionType;
import edu.aiplatform.quizzes.domain.Difficulty;
import edu.aiplatform.quizzes.domain.IQuestionRepository.BankQuery;
import edu.aiplatform.quizzes.domain.IQuestionRepository.BankRow;
import edu.aiplatform.quizzes.domain.Question;
import edu.aiplatform.quizzes.domain.QuestionContent;
import edu.aiplatform.quizzes.domain.QuestionOption;
import edu.aiplatform.quizzes.domain.QuestionStatus;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * SQL viết tay của {@link JdbcQuestionRepository} và migration U10 chạy trên PostgreSQL 16 thật, với role
 * {@code migrator}/{@code app} như khi triển khai.
 */
@Testcontainers(disabledWithoutDocker = true)
class JdbcQuestionRepositoryIntegrationTest {

    // Sinh mỗi lần chạy: container chỉ sống trong test, không để chuỗi mật khẩu cố định trong mã.
    private static final String APP_PASSWORD = UUID.randomUUID().toString();
    private static final String MIGRATOR_PASSWORD = UUID.randomUUID().toString();
    // PostgreSQL lưu micro giây; cắt sẵn để so sánh bằng nhau sau khi đọc lại.
    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.MICROS);

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withEnv("POSTGRES_APP_PASSWORD", APP_PASSWORD)
            .withEnv("POSTGRES_MIGRATOR_PASSWORD", MIGRATOR_PASSWORD)
            .withCopyToContainer(rolesScript(), "/docker-entrypoint-initdb.d/01-roles.sh");

    private static HikariDataSource appDataSource;
    private static HikariDataSource adminDataSource;
    private static JdbcClient admin;
    private static JdbcQuestionRepository repository;

    private UUID subject;
    private UUID author;

    @BeforeAll
    static void migrate() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), "migrator", MIGRATOR_PASSWORD)
                .locations("classpath:db/migration").load().migrate();
        appDataSource = dataSource("app", APP_PASSWORD);
        adminDataSource = dataSource(POSTGRES.getUsername(), POSTGRES.getPassword());
        admin = JdbcClient.create(adminDataSource);
        repository = new JdbcQuestionRepository(JdbcClient.create(appDataSource), new ObjectMapper());
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
        admin.sql("truncate questions, quiz_items, quiz_ai_drafts, quizzes, subjects, accounts cascade").update();
        author = UUID.randomUUID();
        subject = UUID.randomUUID();
        admin.sql("insert into accounts (id, school_email, role, status, display_name) values (:id, :email,"
                        + " 'SUBJECT_MANAGER', 'PENDING', 'Chủ nhiệm')")
                .param("id", author).param("email", author + "@fpt.edu.vn").update();
        admin.sql("insert into subjects (id, code, name, status, manager_id, created_at, updated_at)"
                        + " values (:id, 'SWT301', 'Kiểm thử', 'ACTIVE', :manager, now(), now())")
                .param("id", subject).param("manager", author).update();
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

    private static QuestionContent content(String stem, QuestionType type, Difficulty difficulty, String... tags) {
        return QuestionContent.of(type, stem,
                List.of(new QuestionOption("1", "Đúng"), new QuestionOption("2", "Sai")), List.of("1"),
                "Giải thích", new BigDecimal("1.25"), difficulty, List.of(tags));
    }

    private Question draft(String stem) {
        Question q = Question.newBankDraft(subject, content(stem, QuestionType.MCQ_SINGLE, Difficulty.EASY, "chương 1"),
                author, NOW);
        repository.insert(q);
        return q;
    }

    private Question activate(Question q) {
        Question active = q.activate(NOW);
        assertThat(repository.update(active)).isTrue();
        return repository.find(q.id()).orElseThrow();
    }

    @Test
    void insertedQuestionReadsBackWithJsonFields() {
        Question q = draft("Câu một");

        Question read = repository.find(q.id()).orElseThrow();
        assertThat(read).isEqualTo(q);
        assertThat(read.content().options()).extracting(QuestionOption::text).containsExactly("Đúng", "Sai");
        assertThat(read.content().tags()).containsExactly("chương 1");
        assertThat(read.content().defaultPoints()).isEqualByComparingTo("1.25");
    }

    @Test
    void updateNeedsMatchingVersionAndBumpsIt() {
        Question q = draft("Câu một");
        Question changed = q.withContent(content("Câu một sửa", QuestionType.MCQ_SINGLE, Difficulty.HARD), NOW);

        assertThat(repository.update(changed)).isTrue();
        Question read = repository.find(q.id()).orElseThrow();
        assertThat(read.version()).isEqualTo(1);
        assertThat(read.content().stem()).isEqualTo("Câu một sửa");
        assertThat(repository.update(changed)).as("version cũ").isFalse();
    }

    @Test
    void lineageKeepsAtMostOneDraftAndVersionsInOrder() {
        Question active = activate(draft("Câu một"));
        Question v2 = active.nextDraft(repository.maxVersionNo(active.lineageId()) + 1, author, NOW);
        repository.insert(v2);

        assertThat(repository.findBankDraft(active.lineageId())).map(Question::id).hasValue(v2.id());
        assertThat(repository.lineage(active.lineageId())).extracting(Question::versionNo).containsExactly(2, 1);
        Question another = active.nextDraft(3, author, NOW);
        assertThatThrownBy(() -> repository.insert(another)).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void deleteOnlyRemovesDraftsWithMatchingVersion() {
        Question active = activate(draft("Câu một"));
        assertThat(repository.delete(active.id(), active.version())).isFalse();
        Question q = draft("Câu hai");
        assertThat(repository.delete(q.id(), 5)).isFalse();
        assertThat(repository.delete(q.id(), 0)).isTrue();
        assertThat(repository.find(q.id())).isEmpty();
    }

    @Test
    void bankListShowsOneRowPerLineageWithDraftFlagAndFilters() {
        Question first = activate(draft("Phương trình bậc hai"));
        repository.insert(first.nextDraft(2, author, NOW));
        draft("Hệ phương trình");
        Question multi = Question.newBankDraft(subject,
                content("Chọn số nguyên tố", QuestionType.MCQ_MULTI, Difficulty.HARD, "số học"), author, NOW);
        repository.insert(multi);

        var all = repository.searchBank(subject, new BankQuery(null, null, null, null, 0, 20));
        assertThat(all.total()).isEqualTo(3);
        BankRow firstRow = all.items().stream().filter(r -> r.question().lineageId().equals(first.lineageId()))
                .findFirst().orElseThrow();
        assertThat(firstRow.question().id()).as("bản đã kích hoạt mới nhất").isEqualTo(first.id());
        assertThat(firstRow.hasDraft()).isTrue();

        assertThat(repository.searchBank(subject, new BankQuery(QuestionType.MCQ_MULTI, null, null, null, 0, 20))
                .items()).extracting(r -> r.question().id()).containsExactly(multi.id());
        assertThat(repository.searchBank(subject, new BankQuery(null, null, QuestionStatus.ACTIVE, null, 0, 20))
                .total()).isEqualTo(1);
        assertThat(repository.searchBank(subject, new BankQuery(null, null, null, "SỐ HỌC", 0, 20)).items())
                .as("tìm theo thẻ, không phân biệt hoa thường")
                .extracting(r -> r.question().id()).containsExactly(multi.id());
        assertThat(repository.searchBank(subject, new BankQuery(null, null, null, "100%", 0, 20)).total()).isZero();
        var secondPage = repository.searchBank(subject, new BankQuery(null, null, null, null, 1, 2));
        assertThat(secondPage.items()).hasSize(1);
        assertThat(secondPage.total()).isEqualTo(3);
        assertThat(repository.searchBank(subject, new BankQuery(null, null, null, null, 5, 2)).total()).isEqualTo(3);
        assertThat(repository.searchBank(UUID.randomUUID(), new BankQuery(null, null, null, null, 0, 20)).total())
                .isZero();
        assertThat(repository.countQuizzesUsing(first.id())).isZero();
    }

    @Test
    void databaseRejectsInvalidQuestionsEvenIfCodeIsBypassed() {
        Question q = Question.newBankDraft(subject, QuestionContent.of(QuestionType.MCQ_SINGLE, "Câu",
                List.of(new QuestionOption("1", "A"), new QuestionOption("2", "B")), List.of("1", "2"), null,
                BigDecimal.ONE, Difficulty.EASY, List.of()), author, NOW);
        assertThatThrownBy(() -> repository.insert(q)).isInstanceOf(DataIntegrityViolationException.class);
    }
}
