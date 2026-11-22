package edu.aiplatform.quizzes.repository;

import edu.aiplatform.quizzes.model.DrawRule;
import edu.aiplatform.quizzes.model.QuizPaperMode;
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

/** SQL viết tay của {@link QuizRepository} và phần chọn câu của {@link QuestionRepository} trên PostgreSQL 16. */
@Testcontainers(disabledWithoutDocker = true)
class QuizRepositoryIntegrationTest {

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
    private static QuizRepository quizzes;
    private static QuestionRepository questions;
    private static QuizAiDraftRepository drafts;

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
        quizzes = new QuizRepository(app, new ObjectMapper());
        questions = new QuestionRepository(app, new ObjectMapper());
        drafts = new QuizAiDraftRepository(app, new ObjectMapper());
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
        Question q = Question.createBankDraft(subject, QuestionContent.createContent(QuestionType.MCQ_SINGLE, "Câu " + tag,
                List.of(new QuestionOption("1", "A"), new QuestionOption("2", "B")), List.of("1"), null,
                BigDecimal.ONE, difficulty, List.of(tag)), author, NOW);
        questions.insert(q);
        Question active = q.activate(NOW);
        assertThat(questions.updateQuestion(active)).isTrue();
        return questions.findQuestion(q.id()).orElseThrow();
    }

    private Quiz subjectQuiz() {
        Quiz quiz = Quiz.createQuiz(QuizScope.SUBJECT, null, subject, "Luyện tập", author, NOW);
        quizzes.insert(quiz);
        return quiz;
    }

    @Test
    void richInstructionsRoundTripThroughInsertUpdateAndClone() {
        var document = new edu.aiplatform.shared.content.FormattedText(List.of(
                new edu.aiplatform.shared.content.FormattedText.Block(
                        edu.aiplatform.shared.content.FormattedText.Type.UL,
                        edu.aiplatform.shared.content.FormattedText.Align.CENTER, 1,
                        List.of(new edu.aiplatform.shared.content.FormattedText.Run("Mô tả quiz", true, null, null)))));
        Quiz original = Quiz.createQuiz(QuizScope.SUBJECT, null, subject, "Rich quiz", author, NOW)
                .replaceInstructionsDocument(document);
        quizzes.insert(original);
        Quiz stored = quizzes.findQuiz(original.id()).orElseThrow();
        assertThat(stored.instructions()).isEqualTo("Mô tả quiz");
        assertThat(stored.instructionsDocument()).isEqualTo(document);
        assertThat(quizzes.updateQuiz(stored.updateDetails("Renamed", stored.instructions(), stored.settings(), false,
                null, NOW))).isTrue();
        Quiz updated = quizzes.findQuiz(original.id()).orElseThrow();
        assertThat(updated.instructionsDocument()).isEqualTo(document);
        Quiz copy = updated.copyAs(QuizOrigin.CLONE, 1, null, author, NOW);
        quizzes.insert(copy);
        assertThat(quizzes.findQuiz(copy.id()).orElseThrow().instructionsDocument()).isEqualTo(document);
    }

    @Test
    void retiringUnpublishedQuizKeepsPublicationNullAndReopensAsDeletableDraft() {
        Quiz draft = subjectQuiz();
        assertThat(quizzes.updateQuiz(draft.markRetired("Lưu trữ", NOW))).isTrue();
        Quiz retired = quizzes.findQuiz(draft.id()).orElseThrow();
        assertThat(retired.status()).isEqualTo(QuizStatus.RETIRED);
        assertThat(retired.publishedAt()).isNull();
        assertThat(retired.retiredAt()).isEqualTo(NOW);
        assertThat(quizzes.updateQuiz(retired.markReopened(NOW))).isTrue();
        Quiz reopened = quizzes.findQuiz(draft.id()).orElseThrow();
        assertThat(reopened.status()).isEqualTo(QuizStatus.DRAFT);
        assertThat(reopened.publishedAt()).isNull();
        assertThat(quizzes.deleteQuiz(reopened.id(), reopened.version())).isTrue();
    }

    @Test
    void publishedQuizRetiredAtPublicationInstantReopensAsOpenAfterRoundTrip() {
        Quiz draft = subjectQuiz();
        assertThat(quizzes.updateQuiz(draft.markPublished(NOW))).isTrue();
        Quiz open = quizzes.findQuiz(draft.id()).orElseThrow();
        assertThat(quizzes.updateQuiz(open.markRetired("Hết đợt", NOW))).isTrue();
        Quiz retired = quizzes.findQuiz(draft.id()).orElseThrow();
        assertThat(retired.publishedAt()).isEqualTo(retired.retiredAt());
        assertThat(quizzes.updateQuiz(retired.markReopened(NOW))).isTrue();
        Quiz reopened = quizzes.findQuiz(draft.id()).orElseThrow();
        assertThat(reopened.status()).isEqualTo(QuizStatus.OPEN);
        assertThat(reopened.publishedAt()).isEqualTo(NOW);
        assertThat(reopened.retiredAt()).isNull();
        assertThat(quizzes.deleteQuiz(reopened.id(), reopened.version())).isFalse();
    }

    @Test
    void quizWithSettingsAndItemsReadsBack() {
        Quiz quiz = subjectQuiz();
        Question q = activeBank(Difficulty.EASY, "a");
        quizzes.replaceItems(quiz.id(), List.of(new QuizItem(quiz.id(), q.id(), 1, new BigDecimal("2.50"))));
        Quiz changed = quiz.updateDetails("Bài 1", "Đọc kỹ", new QuizSettings(true, false, 45, null, true, true), false,
                null, NOW);

        assertThat(quizzes.updateQuiz(changed)).isTrue();
        Quiz read = quizzes.findQuiz(quiz.id()).orElseThrow();
        assertThat(read.settings()).isEqualTo(changed.settings());
        assertThat(read.version()).isEqualTo(1);
        assertThat(quizzes.updateQuiz(changed)).as("version cũ").isFalse();
        assertThat(quizzes.listItems(quiz.id())).singleElement().satisfies(i -> assertThat(i.points()).isEqualByComparingTo("2.5"));
        var page = quizzes.listSubjectQuizzes(subject, null, "bài", 0, 20);
        assertThat(page.total()).isEqualTo(1);
        assertThat(page.items().get(0).itemCount()).isEqualTo(1);
        assertThat(page.items().get(0).maxScore()).isEqualByComparingTo("2.5");
        assertThat(questions.countQuizzesUsing(q.id())).isEqualTo(1);
    }

    @Test
    void deleteRemovesUnpublishedQuizWithItemsAndInlineQuestionsOnly() {
        Quiz quiz = subjectQuiz();
        Question inline = Question.createInline(quiz.id(), subject, QuestionContent.createContent(QuestionType.MCQ_SINGLE, "Riêng",
                List.of(new QuestionOption("1", "A"), new QuestionOption("2", "B")), List.of("1"), null, BigDecimal.ONE,
                null, null), author, NOW);
        questions.insert(inline);
        quizzes.replaceItems(quiz.id(), List.of(new QuizItem(quiz.id(), inline.id(), 1, BigDecimal.ONE)));
        assertThat(questions.listInline(quiz.id())).hasSize(1);

        assertThat(quizzes.deleteQuiz(quiz.id(), 0)).isTrue();
        assertThat(quizzes.findQuiz(quiz.id())).isEmpty();
        assertThat(questions.findQuestion(inline.id())).isEmpty();

        Quiz open = subjectQuiz().markPublished(NOW);
        quizzes.updateQuiz(open);
        assertThat(quizzes.deleteQuiz(open.id(), 1)).isFalse();
    }

    @Test
    void versionChainFindsOpenSiblingAndAllowsOneDraftVersion() {
        Quiz v1 = subjectQuiz();
        quizzes.updateQuiz(v1.markPublished(NOW));
        Quiz v2 = quizzes.findQuiz(v1.id()).orElseThrow().copyAs(QuizOrigin.NEW_VERSION, 2, null, author, NOW);
        quizzes.insert(v2);

        assertThat(quizzes.findMaxVersionNo(v1.id())).isEqualTo(2);
        assertThat(quizzes.findMaxVersionNo(v2.id())).isEqualTo(2);
        assertThat(quizzes.findOpenInVersionChain(v2.id())).map(Quiz::id).hasValue(v1.id());
        assertThat(quizzes.findOpenInVersionChain(v1.id())).isEmpty();
        Quiz another = quizzes.findQuiz(v1.id()).orElseThrow().copyAs(QuizOrigin.NEW_VERSION, 3, null, author, NOW);
        assertThatThrownBy(() -> quizzes.insert(another)).isInstanceOf(DuplicateKeyException.class);
        assertThat(quizzes.listSubjectQuizzes(subject, List.of(QuizStatus.OPEN))).extracting(s -> s.quiz().id())
                .containsExactly(v1.id());
    }

    @Test
    void activePicksSkipExistingLineagesAndFilterByDifficultyAndTag() {
        Question easy = activeBank(Difficulty.EASY, "đại số");
        Question hard = activeBank(Difficulty.HARD, "hình học");
        Question retired = activeBank(Difficulty.EASY, "đại số");
        assertThat(questions.updateQuestion(retired.retire(NOW))).isTrue();

        PickQuery all = new PickQuery(null, null, null, Set.of(), 0, 20);
        assertThat(questions.countActive(subject, all)).isEqualTo(2);
        assertThat(questions.searchActive(subject, new PickQuery(null, null, null, Set.of(easy.lineageId()), 0, 20)).items())
                .extracting(r -> r.question().id()).containsExactly(hard.id());
        assertThat(questions.pickRandomActive(subject, new PickQuery(Difficulty.EASY, null, null, Set.of(), 0, 5), 5))
                .extracting(Question::id).containsExactly(easy.id());
        assertThat(questions.countActive(subject, new PickQuery(null, "HÌNH HỌC", null, Set.of(), 0, 20))).isEqualTo(1);
    }

    @Test
    void drawRulesRoundTripAndDriveSummaryOfGeneratedQuiz() {
        Quiz quiz = subjectQuiz();
        quizzes.replaceDrawRules(quiz.id(), List.of(
                new DrawRule(quiz.id(), 1, "Chương 1", null, 4, new BigDecimal("0.50")),
                new DrawRule(quiz.id(), 2, "Chương 2", Difficulty.HARD, 3, new BigDecimal("2.00"))));
        quizzes.updateQuiz(quiz.changePaperMode(QuizPaperMode.GENERATED, NOW));

        assertThat(quizzes.findQuiz(quiz.id()).orElseThrow().paperMode()).isEqualTo(QuizPaperMode.GENERATED);
        assertThat(quizzes.drawRules(quiz.id())).extracting(DrawRule::tag).containsExactly("Chương 1", "Chương 2");
        assertThat(quizzes.drawRules(quiz.id()).get(1).difficulty()).isEqualTo(Difficulty.HARD);
        var summary = quizzes.listSubjectQuizzes(subject, null, null, 0, 20).items().get(0);
        assertThat(summary.itemCount()).isEqualTo(7);
        assertThat(summary.maxScore()).isEqualByComparingTo("8");
    }

    @Test
    void aiDraftRoundTripsAndTransitionsOnlyFromExpectedStatus() {
        Quiz quiz = subjectQuiz();
        QuizAiDraft pending = QuizAiDraft.createPending(DraftTarget.QUIZ, quiz.id(), subject, 3L,
                Map.of(QuizAiDraft.REQUEST_REF, "req-1", "source", "SHARED"), author, NOW);
        drafts.insert(pending);
        UUID task = UUID.randomUUID();
        drafts.attachTask(pending.id(), task);
        drafts.attachTask(pending.id(), UUID.randomUUID());

        assertThat(drafts.findByRequest(author, "req-1")).map(QuizAiDraft::id).contains(pending.id());
        assertThat(drafts.findByRequest(UUID.randomUUID(), "req-1")).isEmpty();
        QuizAiDraft stored = drafts.findDraft(pending.id()).orElseThrow();
        assertThat(stored.taskId()).isEqualTo(task);
        assertThat(stored.baseVersion()).isEqualTo(3L);

        DraftQuestion question = DraftQuestion.createDraftQuestion(QuestionContent.createContent(QuestionType.MCQ_MULTI, "Chọn số chẵn",
                List.of(new QuestionOption("1", "2"), new QuestionOption("2", "3")), List.of("1"), "Vì 2 chẵn",
                BigDecimal.ONE, Difficulty.EASY, List.of()));
        assertThat(drafts.transitionDraft(stored.markReady(List.of(question), Map.of()), DraftStatus.PENDING)).isTrue();
        assertThat(drafts.transitionDraft(stored.markFailed("X", null, NOW), DraftStatus.PENDING)).isFalse();

        QuizAiDraft ready = drafts.findDraft(pending.id()).orElseThrow();
        assertThat(ready.status()).isEqualTo(DraftStatus.READY);
        assertThat(ready.taskId()).isEqualTo(task);
        assertThat(ready.proposal()).containsExactly(question);
        assertThat(drafts.listOpenForQuiz(quiz.id())).extracting(QuizAiDraft::id).containsExactly(pending.id());

        assertThat(drafts.transitionDraft(ready.recordDecision(DraftStatus.ACCEPTED, NOW), DraftStatus.READY)).isTrue();
        assertThat(drafts.listOpenForQuiz(quiz.id())).isEmpty();
        assertThat(drafts.findDraft(pending.id()).orElseThrow().decidedAt()).isEqualTo(NOW);
    }

    @Test
    void sourceRetryDraftLockSerializesSubmissionsAndRetryMetadataRollsBack() throws Exception {
        Quiz quiz = subjectQuiz();
        QuizAiDraft pending = QuizAiDraft.createPending(DraftTarget.QUIZ, quiz.id(), subject, 0L,
                Map.of("requestedCount", 5), author, NOW).retryFromSources(List.of("Nguồn không khớp"));
        drafts.insert(pending);
        try (var first = appDataSource.getConnection(); var second = appDataSource.getConnection()) {
            first.setAutoCommit(false);
            second.setAutoCommit(false);
            var lockedRepository = new QuizAiDraftRepository(JdbcClient.create(
                    new org.springframework.jdbc.datasource.SingleConnectionDataSource(first, true)), new ObjectMapper());
            assertThat(lockedRepository.findForUpdate(pending.id())).isPresent();
            try (var timeout = second.createStatement()) { timeout.execute("set local lock_timeout='100ms'"); }
            try (var query = second.prepareStatement("select id from quiz_ai_drafts where id=? for update")) {
                query.setObject(1, pending.id());
                org.assertj.core.api.Assertions.assertThatThrownBy(query::executeQuery)
                        .isInstanceOf(java.sql.SQLException.class)
                        .satisfies(failure -> assertThat(((java.sql.SQLException) failure).getSQLState()).isEqualTo("55P03"));
            }
            second.rollback();
            UUID retryTask = UUID.randomUUID();
            assertThat(lockedRepository.transitionDraft(pending.attachSourceRetryTask(retryTask), DraftStatus.PENDING)).isTrue();
            first.rollback();
        }
        QuizAiDraft restored = drafts.findDraft(pending.id()).orElseThrow();
        assertThat(restored.needsSourceRetry()).isTrue();
        assertThat(restored.hasSourceRetryAttempt()).isFalse();
        assertThat(restored.taskId()).isNull();
    }

    @Test
    void bankDraftsListedBySubjectOnly() {
        QuizAiDraft bank = QuizAiDraft.createPending(DraftTarget.BANK, null, subject, null, Map.of(), author, NOW);
        drafts.insert(bank);
        drafts.insert(QuizAiDraft.createPending(DraftTarget.QUIZ, subjectQuiz().id(), subject, 0L, Map.of(), author, NOW));

        assertThat(drafts.listOpenForBank(subject)).extracting(QuizAiDraft::id).containsExactly(bank.id());
        assertThat(drafts.findDraft(bank.id()).orElseThrow().baseVersion()).isNull();
    }
}
