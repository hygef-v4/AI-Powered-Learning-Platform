package edu.aiplatform.quizzes.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.academics.contract.IClassAccess;
import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.AuthorizationDecision;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.IClassScope;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.identity.contract.ScopeKind;
import edu.aiplatform.jobs.contract.DomainEvent;
import edu.aiplatform.jobs.contract.EventTypes;
import edu.aiplatform.quizzes.application.QuizAuthoringService.Details;
import edu.aiplatform.quizzes.application.QuizAuthoringService.ItemInput;
import edu.aiplatform.quizzes.application.QuizViews.QuizDetail;
import edu.aiplatform.quizzes.contract.IQuizLifecycleListener;
import edu.aiplatform.quizzes.contract.QuestionType;
import edu.aiplatform.quizzes.contract.QuizAnswer;
import edu.aiplatform.quizzes.contract.QuizScope;
import edu.aiplatform.quizzes.contract.QuizSettings;
import edu.aiplatform.quizzes.contract.QuizStatus;
import edu.aiplatform.quizzes.domain.Difficulty;
import edu.aiplatform.quizzes.domain.Question;
import edu.aiplatform.quizzes.domain.Quiz;
import edu.aiplatform.quizzes.domain.QuestionContent;
import edu.aiplatform.quizzes.domain.QuestionOption;
import edu.aiplatform.quizzes.domain.QuestionScope;
import edu.aiplatform.quizzes.domain.Violation;
import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.subjects.contract.ISubjectQuery;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;

/** UC 48 trên repository giả: soạn, duyệt, phát hành, ngưng, copy, chấm (BR-U10-10…31, P2, P3). */
class QuizServicesTest {

    private static final Instant NOW = Instant.parse("2026-10-20T08:00:00Z");
    private final UUID subject = UUID.randomUUID();
    private final UUID classA = UUID.randomUUID();
    private final UUID classB = UUID.randomUUID();
    private final Actor manager = new Actor(UUID.randomUUID(), Role.SUBJECT_MANAGER);
    private final Actor teacher = new Actor(UUID.randomUUID(), Role.TEACHER);
    private final Actor otherTeacher = new Actor(UUID.randomUUID(), Role.TEACHER);
    private final Map<UUID, ClassStatus> classStatus = new HashMap<>(Map.of(classA, ClassStatus.OPEN, classB, ClassStatus.OPEN));
    private final List<AuditEvent> audits = new ArrayList<>();
    private final List<DomainEvent> events = new ArrayList<>();
    private final List<UUID> retiredNotices = new ArrayList<>();
    private InMemoryQuizStore store;
    private QuizAuthoringService authoring;
    private QuizPublicationService publication;
    private QuizCopyService copies;
    private QuizScorer scorer;
    private QuizQueryService query;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        store = new InMemoryQuizStore();
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        IAuthorization authorization = (actor, action, ref) -> {
            boolean allowed = ref.scope() == ScopeKind.SUBJECT
                    ? actor.equals(manager) && subject.equals(ref.id())
                    : actor.equals(teacher) && (classA.equals(ref.id()) || classB.equals(ref.id()));
            return allowed ? AuthorizationDecision.allow() : AuthorizationDecision.deny("OUT_OF_SCOPE");
        };
        IClassAccess classes = mock(IClassAccess.class);
        when(classes.getClassRef(any())).thenAnswer(call -> {
            UUID id = call.getArgument(0);
            return classStatus.containsKey(id)
                    ? Optional.of(new ClassRef(id, subject, "SE1801", "Lớp", classStatus.get(id), teacher.accountId(), false))
                    : Optional.empty();
        });
        ISubjectQuery subjects = mock(ISubjectQuery.class);
        when(subjects.isActive(subject)).thenReturn(true);
        IClassScope classScope = mock(IClassScope.class);
        when(classScope.listTaughtClasses(teacher.accountId())).thenReturn(List.of(classA, classB));
        ObjectProvider<IQuizLifecycleListener> listeners = mock(ObjectProvider.class);
        when(listeners.orderedStream()).thenAnswer(call -> Stream.of((IQuizLifecycleListener) retiredNotices::add));

        QuizAudit audit = new QuizAudit(audits::add, clock);
        QuizAccess access = new QuizAccess(authorization, audit);
        QuizSupport support = new QuizSupport(store.quizRepository, store.questionRepository, classes, access,
                new ActiveSubjects(subjects));
        QuizReferenceMaterials materials = new QuizReferenceMaterials(new FakeReferenceMaterial(), support, access);
        authoring = new QuizAuthoringService(store.quizRepository, store.questionRepository, support, access, audit,
                materials, clock);
        publication = new QuizPublicationService(store.quizRepository, support, listeners, events::add, audit, clock);
        copies = new QuizCopyService(store.quizRepository, store.questionRepository, classScope, support, access, audit, clock);
        scorer = new QuizScorer(store.quizRepository, store.questionRepository);
        query = new QuizQueryService(store.quizRepository, store.questionRepository, classes);
    }

    private static QuestionContent content(QuestionType type, List<String> correct, String... options) {
        List<QuestionOption> list = new ArrayList<>();
        for (int i = 0; i < options.length; i++) {
            list.add(new QuestionOption(String.valueOf(i + 1), options[i]));
        }
        return QuestionContent.of(type, "Câu " + UUID.randomUUID(), list, correct, "Vì vậy", new BigDecimal("2"),
                Difficulty.EASY, List.of("chương 1"));
    }

    private Question bank(boolean active) {
        Question q = Question.newBankDraft(subject, content(QuestionType.MCQ_SINGLE, List.of("1"), "Đúng", "Sai"),
                manager.accountId(), NOW);
        if (active) {
            q = q.activate(NOW);
        }
        store.put(q);
        return q;
    }

    private static ApiException api(Runnable call) {
        try {
            call.run();
        } catch (ApiException failure) {
            return failure;
        }
        throw new AssertionError("Không ném ApiException");
    }

    private QuizDetail classQuizWith(Question... questions) {
        QuizDetail quiz = authoring.createForClass(teacher, classA, "Luyện tập chương 1");
        List<ItemInput> items = Stream.of(questions).map(q -> new ItemInput(q.id(), null)).toList();
        return authoring.saveItems(teacher, quiz.quiz().id(), items, quiz.quiz().version());
    }

    private QuizDetail published(QuizDetail quiz) {
        QuizDetail reviewed = publication.review(teacher, quiz.quiz().id(), quiz.quiz().version());
        return publication.publish(teacher, quiz.quiz().id(), reviewed.quiz().version());
    }

    @Test
    void createdQuizIsDraftPracticeWithDefaultSettings() {
        QuizDetail quiz = authoring.createForClass(teacher, classA, "Luyện tập");

        assertThat(quiz.quiz().status()).isEqualTo(QuizStatus.DRAFT);
        assertThat(quiz.quiz().scope()).isEqualTo(QuizScope.CLASS);
        assertThat(quiz.quiz().subjectId()).isEqualTo(subject);
        assertThat(quiz.quiz().settings()).isEqualTo(new QuizSettings(false, false, null, 1, true, false));
        assertThat(quiz.canManage()).isTrue();
        assertThat(audits).extracting(AuditEvent::action).containsExactly("QUIZ_CREATED");

        classStatus.put(classA, ClassStatus.ARCHIVED);
        assertThat(api(() -> authoring.createForClass(teacher, classA, "Khác")).code()).isEqualTo("CLASS_ARCHIVED");
    }

    @Test
    void activityMonitoringNeedsNoticeOnly() {
        Question q = bank(true);
        QuizDetail quiz = classQuizWith(q);
        UUID id = quiz.quiz().id();
        ApiException noNotice = api(() -> authoring.updateDetails(teacher, id, new Details("Luyện tập", null,
                Quiz.DEFAULT_SETTINGS, true, "  ", List.of()), quiz.quiz().version()));
        assertThat(((RuleViolationException) noNotice).violations()).extracting(Violation::code)
                .containsExactly("ACTIVITY_NOTICE_REQUIRED");
        QuizDetail enabled = authoring.updateDetails(teacher, id, new Details("Luyện tập", null, Quiz.DEFAULT_SETTINGS,
                true, "Hệ thống ghi lại khi bạn rời trang", List.of()), quiz.quiz().version());
        assertThat(enabled.quiz().activityMonitoringEnabled()).isTrue();
        QuizDetail disabled = authoring.updateDetails(teacher, id, new Details("Luyện tập", null, Quiz.DEFAULT_SETTINGS,
                false, null, List.of()), enabled.quiz().version());
        assertThat(disabled.quiz().activityMonitoringEnabled()).isFalse();
    }

    @Test
    void teacherOfAnotherClassGetsNotFoundAndIsAudited() {
        QuizDetail quiz = authoring.createForClass(teacher, classA, "Luyện tập");
        audits.clear();

        ApiException failure = api(() -> authoring.get(otherTeacher, quiz.quiz().id(), null));

        assertThat(failure.status()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(audits).extracting(AuditEvent::action).containsExactly("ACCESS_DENIED");
    }

    @Test
    void teacherViewsPublishedSubjectQuizFromTheirClassButCannotEditIt() {
        Question q = bank(true);
        QuizDetail draft = authoring.createForSubject(manager, subject, "Quiz của môn");
        draft = authoring.saveItems(manager, draft.quiz().id(), List.of(new ItemInput(q.id(), null)), draft.quiz().version());
        UUID id = draft.quiz().id();
        assertThat(api(() -> authoring.get(teacher, id, classA)).status()).as("chưa phát hành").isEqualTo(HttpStatus.NOT_FOUND);

        QuizDetail reviewed = publication.review(manager, id, draft.quiz().version());
        publication.publish(manager, id, reviewed.quiz().version());

        QuizDetail seen = authoring.get(teacher, id, classA);
        assertThat(seen.canManage()).isFalse();
        assertThat(api(() -> authoring.updateDetails(teacher, id, new Details("x", null, seen.quiz().settings(), false, null,
                List.of()), seen.quiz().version())).status()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(authoring.listClass(teacher, classA)).singleElement()
                .satisfies(row -> assertThat(row.readOnly()).isTrue());
    }

    @Test
    void savingItemsPinsActiveBankVersionsWithDefaultPointsAndReturnsReviewedQuizToDraft() {
        Question active = bank(true);
        Question draftOnly = bank(false);
        QuizDetail quiz = authoring.createForClass(teacher, classA, "Luyện tập");

        RuleViolationException rejected = (RuleViolationException) api(() -> authoring.saveItems(teacher, quiz.quiz().id(),
                List.of(new ItemInput(active.id(), null), new ItemInput(draftOnly.id(), null)), quiz.quiz().version()));
        assertThat(rejected.violations()).extracting(Violation::code).containsExactly("QUESTION_NOT_ACTIVE");

        QuizDetail saved = authoring.saveItems(teacher, quiz.quiz().id(), List.of(new ItemInput(active.id(), null)),
                quiz.quiz().version());
        assertThat(saved.items()).singleElement().satisfies(item -> {
            assertThat(item.question().id()).isEqualTo(active.id());
            assertThat(item.points()).isEqualByComparingTo("2");
        });
        assertThat(api(() -> authoring.saveItems(teacher, quiz.quiz().id(), List.of(), quiz.quiz().version())).code())
                .isEqualTo("QUIZ_VERSION_CONFLICT");

        QuizDetail reviewed = publication.review(teacher, quiz.quiz().id(), saved.quiz().version());
        QuizDetail edited = authoring.saveItems(teacher, quiz.quiz().id(),
                List.of(new ItemInput(active.id(), new BigDecimal("3.5"))), reviewed.quiz().version());
        assertThat(edited.quiz().status()).isEqualTo(QuizStatus.DRAFT);
        assertThat(edited.maxScore()).isEqualByComparingTo("3.5");
    }

    @Test
    void itemsRejectDuplicatesAndBadPoints() {
        Question q = bank(true);
        QuizDetail quiz = authoring.createForClass(teacher, classA, "Luyện tập");
        RuleViolationException failure = (RuleViolationException) api(() -> authoring.saveItems(teacher, quiz.quiz().id(),
                List.of(new ItemInput(q.id(), new BigDecimal("1.234")), new ItemInput(q.id(), null)), quiz.quiz().version()));
        assertThat(failure.violations()).extracting(Violation::code).contains("ITEM_DUPLICATE", "POINTS_INVALID");
    }

    @Test
    void inlineQuestionsAreAddedEditedAndRemovedWithTheQuiz() {
        QuizDetail quiz = authoring.createForClass(teacher, classA, "Luyện tập");
        QuizDetail added = authoring.addInline(teacher, quiz.quiz().id(),
                content(QuestionType.MCQ_MULTI, List.of("1", "2"), "2", "3", "4"), quiz.quiz().version());
        UUID inlineId = added.items().get(0).question().id();
        assertThat(store.questions.get(inlineId).scope()).isEqualTo(QuestionScope.QUIZ);

        QuizDetail updated = authoring.updateInline(teacher, quiz.quiz().id(), inlineId,
                content(QuestionType.MCQ_SINGLE, List.of("1"), "A", "B"), added.quiz().version());
        assertThat(updated.items().get(0).question().type()).isEqualTo(QuestionType.MCQ_SINGLE);

        QuizDetail removed = authoring.deleteInline(teacher, quiz.quiz().id(), inlineId, updated.quiz().version());
        assertThat(removed.items()).isEmpty();
        assertThat(store.questions).doesNotContainKey(inlineId);
    }

    @Test
    void settingsAreValidated() {
        QuizDetail quiz = authoring.createForClass(teacher, classA, "Luyện tập");
        UUID id = quiz.quiz().id();
        long version = quiz.quiz().version();
        RuleViolationException invalid = (RuleViolationException) api(() -> authoring.updateDetails(teacher, id,
                new Details(" ", null, new QuizSettings(true, true, 301, 11, true, true), false, null, List.of()), version));
        assertThat(invalid.violations()).extracting(Violation::code)
                .contains("REQUIRED", "TIME_LIMIT_INVALID", "ATTEMPTS_INVALID");

        QuizDetail ok = authoring.updateDetails(teacher, id, new Details("Luyện tập 2", " Đọc kỹ ",
                new QuizSettings(true, false, 30, null, true, true), false, null, List.of()), version);
        assertThat(ok.quiz().title()).isEqualTo("Luyện tập 2");
        assertThat(ok.quiz().instructions()).isEqualTo("Đọc kỹ");
        assertThat(ok.quiz().settings().maxAttempts()).isNull();
    }

    @Test
    void randomPreviewSkipsQuestionsAlreadyInQuizAndRespectsFreeSlots() {
        Question inQuiz = bank(true);
        Question other = bank(true);
        QuizDetail quiz = classQuizWith(inQuiz);

        var preview = authoring.randomPreview(teacher, quiz.quiz().id(), 5, null, null);
        assertThat(preview.questions()).extracting(v -> v.id()).containsExactly(other.id());
        assertThat(preview.matching()).isEqualTo(1);
        assertThat(preview.freeSlots()).isEqualTo(199);
        assertThat(api(() -> authoring.randomPreview(teacher, quiz.quiz().id(), 200, null, null)).code())
                .isEqualTo("QUIZ_RANDOM_COUNT_INVALID");
        assertThat(authoring.bankQuestions(teacher, quiz.quiz().id(), null, null, null, null, null).items())
                .extracting(r -> r.id()).containsExactly(other.id());
    }

    @Test
    void reviewListsProblemsPerQuestionAndNeedsAtLeastOneQuestion() {
        QuizDetail empty = authoring.createForClass(teacher, classA, "Rỗng");
        RuleViolationException none = (RuleViolationException) api(() -> publication.review(teacher, empty.quiz().id(),
                empty.quiz().version()));
        assertThat(none.code()).isEqualTo("QUIZ_REVIEW_FAILED");

        Question q = bank(true);
        QuizDetail quiz = classQuizWith(q);
        store.put(q.retire(NOW));
        RuleViolationException retired = (RuleViolationException) api(() -> publication.review(teacher, quiz.quiz().id(),
                quiz.quiz().version()));
        assertThat(retired.violations()).singleElement().satisfies(v -> {
            assertThat(v.field()).isEqualTo("items[1]");
            assertThat(v.code()).isEqualTo("QUESTION_NOT_ACTIVE");
        });
    }

    @Test
    void publishNeedsOpenClassThenOpensAndAnnounces() {
        QuizDetail quiz = classQuizWith(bank(true));
        QuizDetail reviewed = publication.review(teacher, quiz.quiz().id(), quiz.quiz().version());
        classStatus.put(classA, ClassStatus.DRAFT);
        assertThat(api(() -> publication.publish(teacher, quiz.quiz().id(), reviewed.quiz().version())).code())
                .isEqualTo("CLASS_NOT_OPEN");
        classStatus.put(classA, ClassStatus.OPEN);

        QuizDetail open = publication.publish(teacher, quiz.quiz().id(), reviewed.quiz().version());

        assertThat(open.quiz().status()).isEqualTo(QuizStatus.OPEN);
        assertThat(open.quiz().publishedAt()).isEqualTo(NOW);
        assertThat(events).singleElement().satisfies(e -> {
            assertThat(e.eventType()).isEqualTo(EventTypes.QUIZ_PUBLISHED);
            assertThat(e.payload()).containsEntry("classId", classA.toString());
        });
        assertThat(api(() -> authoring.saveItems(teacher, quiz.quiz().id(), List.of(), open.quiz().version())).code())
                .isEqualTo("QUIZ_NOT_EDITABLE");
        assertThat(api(() -> authoring.delete(teacher, quiz.quiz().id(), open.quiz().version())).code())
                .isEqualTo("QUIZ_DELETE_BLOCKED");
    }

    @Test
    void publishingNewVersionRetiresTheOldOneAndNotifiesListeners() {
        QuizDetail v1 = published(classQuizWith(bank(true)));
        QuizDetail v2 = copies.newVersion(teacher, v1.quiz().id());
        assertThat(v2.quiz().versionNo()).isEqualTo(2);
        assertThat(api(() -> copies.newVersion(teacher, v1.quiz().id())).code()).isEqualTo("QUIZ_VERSION_EXISTS");

        published(v2);

        assertThat(store.quizzes.get(v1.quiz().id()).status()).isEqualTo(QuizStatus.RETIRED);
        assertThat(retiredNotices).containsExactly(v1.quiz().id());
        assertThat(audits).extracting(AuditEvent::action).contains("QUIZ_VERSION_CREATED", "QUIZ_RETIRED");
        assertThat(copies.newVersion(teacher, v2.quiz().id()).quiz().versionNo()).as("v2 đã mở, được tạo v3")
                .isEqualTo(3);
    }

    @Test
    void retireNeedsReasonAndNotifiesListeners() {
        QuizDetail open = published(classQuizWith(bank(true)));
        assertThat(api(() -> publication.retire(teacher, open.quiz().id(), " ", open.quiz().version())).code())
                .isEqualTo("QUIZ_INVALID");

        QuizDetail retired = publication.retire(teacher, open.quiz().id(), "Đổi đề", open.quiz().version());

        assertThat(retired.quiz().status()).isEqualTo(QuizStatus.RETIRED);
        assertThat(retired.quiz().retiredReason()).isEqualTo("Đổi đề");
        assertThat(retiredNotices).containsExactly(open.quiz().id());
    }

    @Test
    void cloneKeepsPinnedBankVersionsAndCopiesInlineQuestions() {
        Question q = bank(true);
        QuizDetail quiz = classQuizWith(q);
        QuizDetail withInline = authoring.addInline(teacher, quiz.quiz().id(),
                content(QuestionType.MCQ_SINGLE, List.of("1"), "A", "B"), quiz.quiz().version());
        UUID inline = withInline.items().get(1).question().id();

        QuizDetail copy = copies.cloneQuiz(teacher, quiz.quiz().id());

        assertThat(copy.quiz().id()).isNotEqualTo(quiz.quiz().id());
        assertThat(copy.quiz().status()).isEqualTo(QuizStatus.DRAFT);
        assertThat(copy.items()).hasSize(2);
        assertThat(copy.items().get(0).question().id()).isEqualTo(q.id());
        assertThat(copy.items().get(1).question().id()).isNotEqualTo(inline);
        assertThat(store.questions.get(copy.items().get(1).question().id()).quizId()).isEqualTo(copy.quiz().id());
        assertThat(api(() -> copies.newVersion(teacher, quiz.quiz().id())).code()).isEqualTo("QUIZ_NOT_PUBLISHED");
    }

    @Test
    void copyBetweenClassesNeedsTeacherOfBothClasses() {
        QuizDetail source = classQuizWith(bank(true));
        assertThat(copies.copySources(teacher, classB)).extracting(r -> r.id()).containsExactly(source.quiz().id());

        QuizDetail copy = copies.copyToClass(teacher, classB, source.quiz().id());

        assertThat(copy.quiz().classId()).isEqualTo(classB);
        assertThat(audits).extracting(AuditEvent::action).contains("QUIZ_COPIED");
        assertThat(api(() -> copies.copyToClass(otherTeacher, classB, source.quiz().id())).status())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deletingDraftRemovesItsInlineQuestions() {
        QuizDetail quiz = authoring.createForClass(teacher, classA, "Nháp");
        QuizDetail withInline = authoring.addInline(teacher, quiz.quiz().id(),
                content(QuestionType.MCQ_SINGLE, List.of("1"), "A", "B"), quiz.quiz().version());

        authoring.delete(teacher, quiz.quiz().id(), withInline.quiz().version());

        assertThat(store.quizzes).doesNotContainKey(quiz.quiz().id());
        assertThat(store.questions).isEmpty();
        assertThat(audits).extracting(AuditEvent::action).contains("QUIZ_DELETED");
    }

    @Test
    void scoringNeedsExactAnswerSetAndStudentViewHidesAnswers() {
        Question single = bank(true);
        QuizDetail quiz = authoring.createForClass(teacher, classA, "Chấm");
        QuizDetail multi = authoring.addInline(teacher, quiz.quiz().id(),
                content(QuestionType.MCQ_MULTI, List.of("1", "3"), "2", "4", "5"), quiz.quiz().version());
        QuizDetail full = authoring.saveItems(teacher, quiz.quiz().id(), List.of(new ItemInput(single.id(), null),
                new ItemInput(multi.items().get(0).question().id(), new BigDecimal("3"))), multi.quiz().version());
        UUID multiId = multi.items().get(0).question().id();

        var partial = scorer.score(quiz.quiz().id(), List.of(new QuizAnswer(single.id(), Set.of("1")),
                new QuizAnswer(multiId, Set.of("1"))));
        assertThat(partial.total()).isEqualByComparingTo("2");
        assertThat(partial.maxTotal()).isEqualByComparingTo("5");
        assertThat(partial.questions()).extracting(s -> s.correct()).containsExactly(true, false);
        var exact = scorer.score(quiz.quiz().id(), List.of(new QuizAnswer(multiId, Set.of("3", "1"))));
        assertThat(exact.total()).isEqualByComparingTo("3");
        assertThat(scorer.answerReview(quiz.quiz().id()).answers()).extracting(k -> k.correctOptionIds())
                .containsExactly(Set.of("1"), Set.of("1", "3"));

        var student = query.getStudentView(quiz.quiz().id()).orElseThrow();
        assertThat(student.questions()).hasSize(2);
        assertThat(student.quiz().maxScore()).isEqualByComparingTo(full.maxScore());
        assertThat(student.toString()).doesNotContain("Vì vậy");
    }

    @Test
    void studentsSeeOpenClassAndSubjectQuizzesOnly() {
        QuizDetail open = published(classQuizWith(bank(true)));
        authoring.createForClass(teacher, classA, "Còn nháp");
        QuizDetail subjectQuiz = authoring.createForSubject(manager, subject, "Của môn");
        subjectQuiz = authoring.saveItems(manager, subjectQuiz.quiz().id(),
                List.of(new ItemInput(bank(true).id(), null)), subjectQuiz.quiz().version());
        QuizDetail reviewed = publication.review(manager, subjectQuiz.quiz().id(), subjectQuiz.quiz().version());
        publication.publish(manager, subjectQuiz.quiz().id(), reviewed.quiz().version());

        assertThat(query.listForClass(classA)).extracting(v -> v.quizId())
                .containsExactlyInAnyOrder(open.quiz().id(), subjectQuiz.quiz().id());
    }
}
