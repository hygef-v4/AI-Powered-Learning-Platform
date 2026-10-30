package edu.aiplatform.quizzes.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;
import edu.aiplatform.quizzes.contract.QuestionType;
import edu.aiplatform.quizzes.contract.QuizScope;
import edu.aiplatform.quizzes.contract.QuizSettings;
import edu.aiplatform.quizzes.contract.QuizStatus;
import edu.aiplatform.quizzes.model.Difficulty;
import edu.aiplatform.quizzes.model.DraftQuestion;
import edu.aiplatform.quizzes.model.DraftStatus;
import edu.aiplatform.quizzes.model.DraftTarget;
import edu.aiplatform.quizzes.repository.IQuestionRepository.PickQuery;
import edu.aiplatform.quizzes.model.Question;
import edu.aiplatform.quizzes.model.QuestionContent;
import edu.aiplatform.quizzes.model.QuestionOption;
import edu.aiplatform.quizzes.model.Quiz;
import edu.aiplatform.quizzes.model.QuizAiDraft;
import edu.aiplatform.quizzes.model.QuizItem;
import edu.aiplatform.quizzes.model.QuizOrigin;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** SQL viết tay của {@link JdbcQuizRepository} và phần chọn câu của {@link JdbcQuestionRepository} trên PostgreSQL 16. */
@Testcontainers(disabledWithoutDocker = true)
class JdbcQuizRepositoryIntegrationTest {

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
    private static JdbcQuizRepository quizzes;
    private static JdbcQuestionRepository questions;
    private static JdbcQuizAiDraftRepository drafts;

    private UUID subject;
    private UUID author;

    @BeforeAll
    static void migrate() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), "migrator", MIGRATOR_PASSWORD)
                .locations("classpath:db/migration").load().migrate();
        appDataSource = dataSource("app", APP_PASSWORD);
        adminDataSource = dataSource(POSTGRES.getUsername(), POSTGRES.getPassword());
        admin = JdbcClient.create(adminDataSource);
        JdbcClient app = JdbcClient.create(appDataSource);
        quizzes = new JdbcQuizRepository(app, new ObjectMapper());
        questions = new JdbcQuestionRepository(app, new ObjectMapper());
        drafts = new JdbcQuizAiDraftRepository(app, new ObjectMapper());
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

    private Question activeBank(Difficulty difficulty, String tag) {
        Question q = Question.newBankDraft(subject, QuestionContent.of(QuestionType.MCQ_SINGLE, "Câu " + tag,
                List.of(new QuestionOption("1", "A"), new QuestionOption("2", "B")), List.of("1"), null,
                BigDecimal.ONE, difficulty, List.of(tag)), author, NOW);
        questions.insert(q);
        Question active = q.activate(NOW);
        assertThat(questions.update(active)).isTrue();
        return questions.find(q.id()).orElseThrow();
    }

    private Quiz subjectQuiz() {
        Quiz quiz = Quiz.create(QuizScope.SUBJECT, null, subject, "Luyện tập", author, NOW);
        quizzes.insert(quiz);
        return quiz;
    }

    @Test
    void retiringUnpublishedQuizKeepsPublicationNullAndReopensAsDeletableDraft() {
        Quiz draft = subjectQuiz();
        assertThat(quizzes.update(draft.retired("Lưu trữ", NOW))).isTrue();
        Quiz retired = quizzes.find(draft.id()).orElseThrow();
        assertThat(retired.status()).isEqualTo(QuizStatus.RETIRED);
        assertThat(retired.publishedAt()).isNull();
        assertThat(retired.retiredAt()).isEqualTo(NOW);
        assertThat(quizzes.update(retired.reopened(NOW))).isTrue();
        Quiz reopened = quizzes.find(draft.id()).orElseThrow();
        assertThat(reopened.status()).isEqualTo(QuizStatus.DRAFT);
        assertThat(reopened.publishedAt()).isNull();
        assertThat(quizzes.delete(reopened.id(), reopened.version())).isTrue();
    }

    @Test
    void publishedQuizRetiredAtPublicationInstantReopensAsOpenAfterRoundTrip() {
        Quiz draft = subjectQuiz();
        assertThat(quizzes.update(draft.reviewed(NOW))).isTrue();
        Quiz reviewed = quizzes.find(draft.id()).orElseThrow();
        assertThat(quizzes.update(reviewed.published(NOW))).isTrue();
        Quiz open = quizzes.find(draft.id()).orElseThrow();
        assertThat(quizzes.update(open.retired("Hết đợt", NOW))).isTrue();
        Quiz retired = quizzes.find(draft.id()).orElseThrow();
        assertThat(retired.publishedAt()).isEqualTo(retired.retiredAt());
        assertThat(quizzes.update(retired.reopened(NOW))).isTrue();
        Quiz reopened = quizzes.find(draft.id()).orElseThrow();
        assertThat(reopened.status()).isEqualTo(QuizStatus.OPEN);
        assertThat(reopened.publishedAt()).isEqualTo(NOW);
        assertThat(reopened.retiredAt()).isNull();
        assertThat(quizzes.delete(reopened.id(), reopened.version())).isFalse();
    }

    @Test
    void quizWithSettingsAndItemsReadsBack() {
        Quiz quiz = subjectQuiz();
        Question q = activeBank(Difficulty.EASY, "a");
        quizzes.replaceItems(quiz.id(), List.of(new QuizItem(quiz.id(), q.id(), 1, new BigDecimal("2.50"))));
        Quiz changed = quiz.withDetails("Bài 1", "Đọc kỹ", new QuizSettings(true, false, 45, null, true, true), false,
                null, List.of(UUID.randomUUID()), NOW);

        assertThat(quizzes.update(changed)).isTrue();
        Quiz read = quizzes.find(quiz.id()).orElseThrow();
        assertThat(read.settings()).isEqualTo(changed.settings());
        assertThat(read.referenceMaterialIds()).isEqualTo(changed.referenceMaterialIds());
        assertThat(read.version()).isEqualTo(1);
        assertThat(quizzes.update(changed)).as("version cũ").isFalse();
        assertThat(quizzes.items(quiz.id())).singleElement().satisfies(i -> assertThat(i.points()).isEqualByComparingTo("2.5"));
        var page = quizzes.listSubjectQuizzes(subject, null, "bài", 0, 20);
        assertThat(page.total()).isEqualTo(1);
        assertThat(page.items().get(0).itemCount()).isEqualTo(1);
        assertThat(page.items().get(0).maxScore()).isEqualByComparingTo("2.5");
        assertThat(questions.countQuizzesUsing(q.id())).isEqualTo(1);
    }

    @Test
    void deleteRemovesUnpublishedQuizWithItemsAndInlineQuestionsOnly() {
        Quiz quiz = subjectQuiz();
        Question inline = Question.newInline(quiz.id(), subject, QuestionContent.of(QuestionType.MCQ_SINGLE, "Riêng",
                List.of(new QuestionOption("1", "A"), new QuestionOption("2", "B")), List.of("1"), null, BigDecimal.ONE,
                null, null), author, NOW);
        questions.insert(inline);
        quizzes.replaceItems(quiz.id(), List.of(new QuizItem(quiz.id(), inline.id(), 1, BigDecimal.ONE)));
        assertThat(questions.inlineOf(quiz.id())).hasSize(1);

        assertThat(quizzes.delete(quiz.id(), 0)).isTrue();
        assertThat(quizzes.find(quiz.id())).isEmpty();
        assertThat(questions.find(inline.id())).isEmpty();

        Quiz published = subjectQuiz().reviewed(NOW);
        quizzes.update(published);
        Quiz open = quizzes.find(published.id()).orElseThrow().published(NOW);
        quizzes.update(open);
        assertThat(quizzes.delete(open.id(), 2)).isFalse();
    }

    @Test
    void versionChainFindsOpenSiblingAndAllowsOneDraftVersion() {
        Quiz v1 = subjectQuiz();
        quizzes.update(v1.reviewed(NOW));
        quizzes.update(quizzes.find(v1.id()).orElseThrow().published(NOW));
        Quiz v2 = quizzes.find(v1.id()).orElseThrow().copyAs(QuizOrigin.NEW_VERSION, 2, null, author, NOW);
        quizzes.insert(v2);

        assertThat(quizzes.maxVersionNo(v1.id())).isEqualTo(2);
        assertThat(quizzes.maxVersionNo(v2.id())).isEqualTo(2);
        assertThat(quizzes.findOpenInVersionChain(v2.id())).map(Quiz::id).hasValue(v1.id());
        assertThat(quizzes.findOpenInVersionChain(v1.id())).isEmpty();
        Quiz another = quizzes.find(v1.id()).orElseThrow().copyAs(QuizOrigin.NEW_VERSION, 3, null, author, NOW);
        assertThatThrownBy(() -> quizzes.insert(another)).isInstanceOf(DuplicateKeyException.class);
        assertThat(quizzes.listSubjectQuizzes(subject, List.of(QuizStatus.OPEN))).extracting(s -> s.quiz().id())
                .containsExactly(v1.id());
    }

    @Test
    void activePicksSkipExistingLineagesAndFilterByDifficultyAndTag() {
        Question easy = activeBank(Difficulty.EASY, "đại số");
        Question hard = activeBank(Difficulty.HARD, "hình học");
        Question retired = activeBank(Difficulty.EASY, "đại số");
        assertThat(questions.update(retired.retire(NOW))).isTrue();

        PickQuery all = new PickQuery(null, null, null, Set.of(), 0, 20);
        assertThat(questions.countActive(subject, all)).isEqualTo(2);
        assertThat(questions.searchActive(subject, new PickQuery(null, null, null, Set.of(easy.lineageId()), 0, 20)).items())
                .extracting(r -> r.question().id()).containsExactly(hard.id());
        assertThat(questions.randomActive(subject, new PickQuery(Difficulty.EASY, null, null, Set.of(), 0, 5), 5))
                .extracting(Question::id).containsExactly(easy.id());
        assertThat(questions.countActive(subject, new PickQuery(null, "HÌNH HỌC", null, Set.of(), 0, 20))).isEqualTo(1);
    }

    @Test
    void openQuizzesAttachedToMaterialAreFound() {
        UUID material = UUID.randomUUID();
        Quiz quiz = subjectQuiz();
        quizzes.update(quiz.withDetails("Có tài liệu", null, quiz.settings(), false, null, List.of(material), NOW)
                .reviewed(NOW));
        quizzes.update(quizzes.find(quiz.id()).orElseThrow().published(NOW));

        assertThat(quizzes.listOpenByMaterial(material, UUID.randomUUID(), subject)).extracting(s -> s.quiz().id())
                .containsExactly(quiz.id());
        assertThat(quizzes.listOpenByMaterial(UUID.randomUUID(), UUID.randomUUID(), subject)).isEmpty();
    }

    @Test
    void aiDraftRoundTripsAndTransitionsOnlyFromExpectedStatus() {
        Quiz quiz = subjectQuiz();
        QuizAiDraft pending = QuizAiDraft.pending(DraftTarget.QUIZ, quiz.id(), subject, 3L,
                Map.of(QuizAiDraft.REQUEST_REF, "req-1", "source", "SHARED"), author, NOW);
        drafts.insert(pending);
        UUID task = UUID.randomUUID();
        drafts.attachTask(pending.id(), task);
        drafts.attachTask(pending.id(), UUID.randomUUID());

        assertThat(drafts.findByRequest(author, "req-1")).map(QuizAiDraft::id).contains(pending.id());
        assertThat(drafts.findByRequest(UUID.randomUUID(), "req-1")).isEmpty();
        QuizAiDraft stored = drafts.find(pending.id()).orElseThrow();
        assertThat(stored.taskId()).isEqualTo(task);
        assertThat(stored.baseVersion()).isEqualTo(3L);

        DraftQuestion question = DraftQuestion.of(QuestionContent.of(QuestionType.MCQ_MULTI, "Chọn số chẵn",
                List.of(new QuestionOption("1", "2"), new QuestionOption("2", "3")), List.of("1"), "Vì 2 chẵn",
                BigDecimal.ONE, Difficulty.EASY, List.of()));
        assertThat(drafts.transition(stored.ready(List.of(question), Map.of()), DraftStatus.PENDING)).isTrue();
        assertThat(drafts.transition(stored.failed("X", null, NOW), DraftStatus.PENDING)).isFalse();

        QuizAiDraft ready = drafts.find(pending.id()).orElseThrow();
        assertThat(ready.status()).isEqualTo(DraftStatus.READY);
        assertThat(ready.taskId()).isEqualTo(task);
        assertThat(ready.proposal()).containsExactly(question);
        assertThat(drafts.listOpenForQuiz(quiz.id())).extracting(QuizAiDraft::id).containsExactly(pending.id());

        assertThat(drafts.transition(ready.decided(DraftStatus.ACCEPTED, NOW), DraftStatus.READY)).isTrue();
        assertThat(drafts.listOpenForQuiz(quiz.id())).isEmpty();
        assertThat(drafts.find(pending.id()).orElseThrow().decidedAt()).isEqualTo(NOW);
    }

    @Test
    void bankDraftsListedBySubjectOnly() {
        QuizAiDraft bank = QuizAiDraft.pending(DraftTarget.BANK, null, subject, null, Map.of(), author, NOW);
        drafts.insert(bank);
        drafts.insert(QuizAiDraft.pending(DraftTarget.QUIZ, subjectQuiz().id(), subject, 0L, Map.of(), author, NOW));

        assertThat(drafts.listOpenForBank(subject)).extracting(QuizAiDraft::id).containsExactly(bank.id());
        assertThat(drafts.find(bank.id()).orElseThrow().baseVersion()).isNull();
    }
}
