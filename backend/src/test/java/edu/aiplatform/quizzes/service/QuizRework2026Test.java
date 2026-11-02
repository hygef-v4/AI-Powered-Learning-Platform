package edu.aiplatform.quizzes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.academics.contract.IClassAccess;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.AuthorizationDecision;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.jobs.contract.DomainEvent;
import edu.aiplatform.jobs.contract.EventTypes;
import edu.aiplatform.quizzes.contract.QuestionType;
import edu.aiplatform.quizzes.contract.QuizAnswer;
import edu.aiplatform.quizzes.contract.QuizStatus;
import edu.aiplatform.quizzes.model.Difficulty;
import edu.aiplatform.quizzes.model.Question;
import edu.aiplatform.quizzes.model.QuestionContent;
import edu.aiplatform.quizzes.model.QuestionOption;
import edu.aiplatform.quizzes.model.QuestionStatus;
import edu.aiplatform.quizzes.model.QuizPaperMode;
import edu.aiplatform.quizzes.service.QuestionBankService.ReviewItem;
import edu.aiplatform.quizzes.service.QuizPaperService.RuleInput;
import edu.aiplatform.quizzes.service.QuizViews.QuizDetail;
import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.subjects.contract.ISubjectQuery;
import edu.aiplatform.subjects.contract.SubjectRef;
import edu.aiplatform.subjects.contract.SubjectStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/** Sinh đề theo quy tắc (BR-U10-17), chống trùng (BR-U10-08), đề xuất và duyệt câu (BR-U10-09). */
class QuizRework2026Test {

    private static final Instant NOW = Instant.parse("2026-11-01T00:00:00Z");

    private final UUID subject = UUID.randomUUID();
    private final UUID classId = UUID.randomUUID();
    private final Actor teacher = new Actor(UUID.randomUUID(), Role.TEACHER);
    private final Actor otherTeacher = new Actor(UUID.randomUUID(), Role.TEACHER);
    private final Actor manager = new Actor(UUID.randomUUID(), Role.SUBJECT_MANAGER);
    private final InMemoryQuizStore store = new InMemoryQuizStore();
    private final List<DomainEvent> events = new ArrayList<>();

    private QuizPaperService papers;
    private QuizPublicationService publication;
    private QuizAuthoringService authoring;
    private QuizQueryService query;
    private QuizScorer scorer;
    private QuestionBankService bank;
    private QuestionProposalService proposals;
    private QuestionImporter importer;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        IAuthorization authorization = mock(IAuthorization.class);
        when(authorization.authorize(any(), anyString(), any())).thenReturn(AuthorizationDecision.allow());
        QuizAudit audit = new QuizAudit(event -> { }, clock);
        QuizAccess access = new QuizAccess(authorization, audit);
        ISubjectQuery subjects = mock(ISubjectQuery.class);
        when(subjects.isActive(subject)).thenReturn(true);
        when(subjects.getSubject(subject)).thenReturn(Optional.of(
                new SubjectRef(subject, "CS", "Môn", SubjectStatus.ACTIVE, manager.accountId())));
        IClassAccess classes = mock(IClassAccess.class);
        when(classes.getClassRef(classId)).thenReturn(Optional.of(
                new ClassRef(classId, subject, "C1", "Lớp 1", ClassStatus.OPEN, teacher.accountId(), false)));
        ActiveSubjects active = new ActiveSubjects(subjects);
        QuizSupport support = new QuizSupport(store.quizRepository, store.questionRepository, classes, access, active);
        papers = new QuizPaperService(store.quizRepository, store.questionRepository, support, clock);
        ObjectProvider<edu.aiplatform.quizzes.contract.IQuizLifecycleListener> listeners = mock(ObjectProvider.class);
        when(listeners.orderedStream()).thenAnswer(call -> java.util.stream.Stream.empty());
        publication = new QuizPublicationService(store.quizRepository, support, papers, listeners, events::add, audit, clock);
        authoring = new QuizAuthoringService(store.quizRepository, store.questionRepository, support, access, audit, clock);
        query = new QuizQueryService(store.quizRepository, store.questionRepository, classes, papers);
        scorer = new QuizScorer(store.quizRepository, store.questionRepository);
        QuestionDuplicates duplicates = new QuestionDuplicates(store.questionRepository);
        bank = new QuestionBankService(store.questionRepository, active, access, audit, duplicates, papers, events::add, clock);
        proposals = new QuestionProposalService(store.questionRepository, support, access, active, duplicates, subjects,
                events::add, audit, clock);
        importer = new QuestionImporter(store.questionRepository, active, access, audit, duplicates, clock);
    }

    private static QuestionContent content(String stem, String... tags) {
        return QuestionContent.of(QuestionType.MCQ_SINGLE, stem, List.of(new QuestionOption("1", "Đúng"),
                new QuestionOption("2", "Sai")), List.of("1"), "Vì vậy", BigDecimal.ONE, Difficulty.EASY, List.of(tags));
    }

    private Question active(String stem, String... tags) {
        Question q = Question.newBankDraft(subject, content(stem, tags), manager.accountId(), NOW).activate(NOW);
        store.put(q);
        return q;
    }

    private void fill(String tag, int count) {
        for (int i = 0; i < count; i++) {
            active("Câu " + tag + " số " + i + " khác hẳn " + UUID.randomUUID(), tag);
        }
    }

    private QuizDetail generatedQuiz(RuleInput... rules) {
        QuizDetail quiz = authoring.createForClass(teacher, classId, "Sinh đề");
        return papers.saveRules(teacher, quiz.quiz().id(), QuizPaperMode.GENERATED, List.of(rules), quiz.quiz().version());
    }

    private static RuleInput rule(String tag, int count) {
        return new RuleInput(tag, null, count, new BigDecimal("0.5"));
    }

    private static ApiException api(Runnable action) {
        return catchThrowableOfType(action::run, ApiException.class);
    }

    // ---- BR-U10-20: bỏ bước duyệt ----

    @Test
    void draftPublishesDirectlyWithoutReviewStep() {
        QuizDetail quiz = generatedQuiz(rule("t", 1));
        fill("t", 1);
        QuizDetail open = publication.publish(teacher, quiz.quiz().id(), quiz.quiz().version());
        assertThat(open.quiz().status()).isEqualTo(QuizStatus.OPEN);
        assertThat(api(() -> publication.publish(teacher, quiz.quiz().id(), open.quiz().version())).code())
                .isEqualTo("QUIZ_NOT_DRAFT");
    }

    // ---- BR-U10-17: sinh đề ----

    @Test
    void eachAttemptDrawsItsOwnPaperAndMaxScoreComesFromRules() {
        fill("t1", 10);
        fill("t2", 10);
        QuizDetail quiz = generatedQuiz(rule("t1", 4), rule("t2", 5));
        assertThat(quiz.maxScore()).isEqualByComparingTo("4.5");
        UUID id = quiz.quiz().id();
        var described = query.getStudentView(id).orElseThrow();
        assertThat(described.generated()).isTrue();
        assertThat(described.questions()).isEmpty();
        assertThat(described.questionCount()).isEqualTo(9);

        Set<Set<UUID>> papersSeen = new HashSet<>();
        for (long seed = 0; seed < 5; seed++) {
            var paper = query.drawPaper(id, Set.of(), seed).orElseThrow();
            assertThat(paper.questions()).hasSize(9);
            assertThat(paper.quiz().maxScore()).isEqualByComparingTo("4.5");
            papersSeen.add(new HashSet<>(paper.questions().stream().map(q -> q.questionId()).toList()));
        }
        assertThat(papersSeen).as("mỗi lượt một đề").hasSizeGreaterThan(1);
    }

    @Test
    void scoringUsesOnlyTheQuestionsOfTheAttempt() {
        Question a = active("Câu a độc nhất", "t");
        Question b = active("Câu b khác hoàn toàn", "t");
        active("Câu c không được giao", "t");
        QuizDetail quiz = generatedQuiz(rule("t", 2));
        Map<UUID, BigDecimal> points = new java.util.LinkedHashMap<>();
        points.put(a.id(), new BigDecimal("2"));
        points.put(b.id(), new BigDecimal("3"));
        var score = scorer.score(quiz.quiz().id(), List.of(new QuizAnswer(a.id(), Set.of("1"))), points);
        assertThat(score.total()).isEqualByComparingTo("2");
        assertThat(score.maxTotal()).as("câu không được giao không tính vào điểm tối đa").isEqualByComparingTo("5");
        assertThat(score.questions()).hasSize(2);
        assertThat(scorer.answerReview(quiz.quiz().id(), points.keySet()).answers()).hasSize(2);
        assertThat(query.getPaper(quiz.quiz().id(), points).orElseThrow().questions()).hasSize(2);
    }

    @Test
    void shortPoolBlocksPublishingAndStartingAndWarnsWhenRetiring() {
        fill("t", 1);
        QuizDetail quiz = generatedQuiz(rule("t", 2));
        UUID id = quiz.quiz().id();
        var failed = (RuleViolationException) api(() -> publication.publish(teacher, id, quiz.quiz().version()));
        assertThat(failed.code()).isEqualTo("QUIZ_PUBLISH_FAILED");
        assertThat(failed.violations()).singleElement().satisfies(v -> {
            assertThat(v.code()).isEqualTo("POOL_INSUFFICIENT");
            assertThat(v.message()).contains("cần 2 câu").contains("cấp được 1");
        });

        Question last = active("Câu cuối của kho", "t");
        QuizDetail open = publication.publish(teacher, id, quiz.quiz().version());
        assertThat(open.quiz().status()).isEqualTo(QuizStatus.OPEN);
        var retired = bank.retire(manager, last.id(), last.version());
        assertThat(retired.quizzesShortOfQuestions()).containsExactly("Sinh đề");
        assertThat(api(() -> query.drawPaper(id, Set.of(), 1L)).code()).isEqualTo("QUIZ_POOL_INSUFFICIENT");
    }

    @Test
    void rulesAreValidatedAndCopiedWithTags() {
        QuizDetail quiz = authoring.createForClass(teacher, classId, "Sai");
        var invalid = (RuleViolationException) api(() -> papers.saveRules(teacher, quiz.quiz().id(), QuizPaperMode.GENERATED,
                List.of(rule("t", 1), rule(" T ", 2), new RuleInput("x", null, 0, BigDecimal.ONE)), quiz.quiz().version()));
        assertThat(invalid.violations()).extracting(v -> v.code()).contains("RULE_DUPLICATE", "COUNT_INVALID");
        fill("Thẻ A", 2);
        assertThat(papers.bankTags(teacher, quiz.quiz().id())).singleElement().satisfies(t -> {
            assertThat(t.tag()).isEqualTo("Thẻ A");
            assertThat(t.activeCount()).isEqualTo(2);
        });
        QuizDetail generated = papers.saveRules(teacher, quiz.quiz().id(), QuizPaperMode.GENERATED, List.of(rule("thẻ a", 3)),
                quiz.quiz().version());
        var preview = papers.preview(teacher, generated.quiz().id());
        assertThat(preview.items()).hasSize(2);
        assertThat(preview.shortages()).singleElement().satisfies(s -> assertThat(s.available()).isEqualTo(2));
    }

    // ---- BR-U10-08: chống trùng ----

    @Test
    void exactDuplicateIsRejectedAndNearDuplicateOnlyWarns() {
        active("Độ phức tạp của sắp xếp nổi bọt là gì?");
        assertThat(api(() -> bank.create(manager, subject, content(" độ phức tạp của  SẮP XẾP nổi bọt là gì? "))).code())
                .isEqualTo("QUESTION_DUPLICATE");
        var near = bank.create(manager, subject, content("Độ phức tạp của sắp xếp nổi bọt là bao nhiêu?"));
        assertThat(near.similar()).singleElement().satisfies(s -> assertThat(s.percent()).isGreaterThanOrEqualTo(70));
    }

    @Test
    void newVersionMayEqualItsOwnPreviousVersion() {
        Question q = active("Câu có phiên bản mới");
        var draft = bank.openDraft(manager, q.id());
        var activated = bank.activate(manager, draft.question().id(), draft.question().version());
        assertThat(activated.question().status()).isEqualTo(QuestionStatus.ACTIVE);
    }

    @Test
    void importReportsDuplicatesPerRow() {
        active("Đã có trong ngân hàng");
        String csv = "noi_dung,loai,lua_chon_1,lua_chon_2,dap_an_dung\n"
                + "Đã có trong ngân hàng,MCQ_SINGLE,Đúng,Sai,1\n"
                + "Câu mới,MCQ_SINGLE,A,B,1\n"
                + "câu MỚI,MCQ_SINGLE,B,A,2\n";
        var result = importer.importFile(manager, subject, csv.getBytes(java.nio.charset.StandardCharsets.UTF_8), false);
        assertThat(result.created()).isEqualTo(1);
        assertThat(result.rows()).extracting(r -> r.valid()).containsExactly(false, true, false);
        assertThat(result.rows().get(0).reasons()).singleElement().satisfies(v -> assertThat(v.code()).isEqualTo("QUESTION_DUPLICATE"));
        assertThat(result.rows().get(2).reasons().get(0).message()).contains("trong tệp");
    }

    // ---- BR-U10-09: đề xuất và duyệt ----

    @Test
    void teacherProposalGoesThroughManagerApproval() {
        var proposed = proposals.propose(teacher, classId, content("Câu giảng viên đề xuất", "t"));
        assertThat(proposed.question().status()).isEqualTo(QuestionStatus.PENDING);
        assertThat(events).singleElement().satisfies(e -> {
            assertThat(e.eventType()).isEqualTo(EventTypes.QUESTION_PROPOSED);
            assertThat(e.payload().get("recipientIds")).isEqualTo(List.of(manager.accountId().toString()));
        });
        assertThat(store.questionRepository.activePool(subject)).as("chưa duyệt thì chưa vào kho").isEmpty();

        UUID id = proposed.question().id();
        assertThat(api(() -> bank.update(manager, id, content("Sửa lén"), proposed.question().version())).code())
                .isEqualTo("QUESTION_IS_PROPOSAL");
        assertThat(api(() -> bank.reject(manager, id, "  ", proposed.question().version())).code())
                .isEqualTo("QUESTION_REASON_INVALID");
        var rejected = bank.reject(manager, id, "Đáp án chưa rõ", proposed.question().version());
        assertThat(rejected.question().status()).isEqualTo(QuestionStatus.REJECTED);
        assertThat(rejected.review().reason()).isEqualTo("Đáp án chưa rõ");
        assertThat(events.get(1).eventType()).isEqualTo(EventTypes.QUESTION_REVIEWED);
        assertThat(events.get(1).payload()).containsEntry("classId", classId.toString());

        var again = proposals.resubmit(teacher, classId, id, content("Câu đã sửa rõ ràng", "t"), rejected.question().version());
        assertThat(again.question().status()).isEqualTo(QuestionStatus.PENDING);
        var approved = bank.approve(manager, id, again.question().version());
        assertThat(approved.question().status()).isEqualTo(QuestionStatus.ACTIVE);
        assertThat(store.questionRepository.activePool(subject)).hasSize(1);
        assertThat(api(() -> proposals.resubmit(teacher, classId, id, content("Sửa sau khi duyệt"), approved.question().version())).code())
                .isEqualTo("QUESTION_NOT_PENDING");
    }

    @Test
    void proposalIsPrivateToItsAuthorAndCanBeWithdrawn() {
        var proposed = proposals.propose(teacher, classId, content("Câu của tôi"));
        UUID id = proposed.question().id();
        assertThat(proposals.list(teacher, classId)).hasSize(1);
        assertThat(proposals.list(otherTeacher, classId)).isEmpty();
        assertThat(api(() -> proposals.withdraw(otherTeacher, classId, id, proposed.question().version())).code())
                .isEqualTo("QUESTION_NOT_FOUND");
        proposals.withdraw(teacher, classId, id, proposed.question().version());
        assertThat(proposals.list(teacher, classId)).isEmpty();
    }

    @Test
    void proposalCannotDuplicateTheBank() {
        active("Câu đã có");
        assertThat(api(() -> proposals.propose(teacher, classId, content("CÂU ĐÃ CÓ"))).code()).isEqualTo("QUESTION_DUPLICATE");
    }

    @Test
    void bulkApproveReportsEachItemAndBulkActivateHandlesDrafts() {
        var first = proposals.propose(teacher, classId, content("Đề xuất số một về mạng"));
        var second = proposals.propose(teacher, classId, content("Đề xuất số hai về cơ sở dữ liệu"));
        var result = bank.approveMany(manager, subject, List.of(new ReviewItem(first.question().id(), first.question().version()),
                new ReviewItem(second.question().id(), 99), new ReviewItem(UUID.randomUUID(), 0)));
        assertThat(result.succeeded()).isEqualTo(1);
        assertThat(result.failed()).isEqualTo(2);
        assertThat(result.items().get(1).code()).isEqualTo("QUESTION_VERSION_CONFLICT");
        assertThat(result.items().get(2).code()).isEqualTo("QUESTION_NOT_FOUND");

        var draft = bank.create(manager, subject, content("Nháp nhập từ tệp"));
        var activated = bank.activateMany(manager, subject, List.of(new ReviewItem(draft.question().id(), draft.question().version())));
        assertThat(activated.succeeded()).isEqualTo(1);
    }

    @Test
    void twoVersionsOfOneQuestionCannotShareAQuiz() {
        Question v1 = active("Câu hai phiên bản");
        var draft = bank.openDraft(manager, v1.id());
        var v2 = bank.activate(manager, draft.question().id(), draft.question().version());
        QuizDetail quiz = authoring.createForClass(teacher, classId, "Quiz");
        var failed = (RuleViolationException) api(() -> authoring.saveItems(teacher, quiz.quiz().id(), List.of(
                new QuizAuthoringService.ItemInput(v1.id(), null), new QuizAuthoringService.ItemInput(v2.question().id(), null)),
                quiz.quiz().version()));
        assertThat(failed.violations()).singleElement().satisfies(v -> assertThat(v.code()).isEqualTo("QUESTION_SAME_LINEAGE"));
    }
}
