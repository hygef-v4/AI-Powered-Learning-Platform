package edu.aiplatform.quizzes.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.academics.contract.IClassAccess;
import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.ai.contract.AiSource;
import edu.aiplatform.ai.contract.AiTaskFailure;
import edu.aiplatform.ai.contract.AiTaskRequest;
import edu.aiplatform.ai.contract.AiTaskResult;
import edu.aiplatform.ai.contract.AiTaskState;
import edu.aiplatform.ai.contract.AiTaskView;
import edu.aiplatform.ai.contract.ApplyOutcome;
import edu.aiplatform.ai.contract.IAiTask;
import edu.aiplatform.ai.contract.McpToolResult;
import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.AuthorizationDecision;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.ResourceRef;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.identity.contract.ScopeKind;
import edu.aiplatform.quizzes.application.AiDraftViews.AiDraftView;
import edu.aiplatform.quizzes.application.AiDraftViews.BankAcceptResult;
import edu.aiplatform.quizzes.application.QuizAiDraftService.DraftRequest;
import edu.aiplatform.quizzes.application.QuizAuthoringService.Details;
import edu.aiplatform.quizzes.application.QuizViews.QuizDetail;
import edu.aiplatform.quizzes.contract.IQuizLifecycleListener;
import edu.aiplatform.quizzes.contract.QuestionType;
import edu.aiplatform.quizzes.domain.Difficulty;
import edu.aiplatform.quizzes.domain.DraftOrigin;
import edu.aiplatform.quizzes.domain.DraftQuestion;
import edu.aiplatform.quizzes.domain.DraftStatus;
import edu.aiplatform.quizzes.domain.DraftTarget;
import edu.aiplatform.quizzes.domain.Question;
import edu.aiplatform.quizzes.domain.QuestionContent;
import edu.aiplatform.quizzes.domain.QuestionOption;
import edu.aiplatform.quizzes.domain.QuestionScope;
import edu.aiplatform.quizzes.domain.QuestionStatus;
import edu.aiplatform.quizzes.domain.Quiz;
import edu.aiplatform.quizzes.domain.QuizItem;
import edu.aiplatform.quizzes.domain.QuizRules;
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
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionOperations;

/** F4, F8, F13 và tài liệu tham khảo (H1–H9) trên repository giả, U06 và U13 giả. */
class QuizAiDraftServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-22T08:00:00Z");
    private static final String OUTPUT = """
            {"questions":[
              {"type":"MCQ_SINGLE","stem":"2 + 2 = ?","options":["3","4"],"correct":[2],"explanation":"Cộng",
               "difficulty":"EASY"},
              {"type":"MCQ_SINGLE","stem":"Câu lỗi","options":["A","A"],"correct":[1,2]}]}""";
    private final UUID subject = UUID.randomUUID();
    private final UUID classA = UUID.randomUUID();
    private final Actor manager = new Actor(UUID.randomUUID(), Role.SUBJECT_MANAGER);
    private final Actor teacher = new Actor(UUID.randomUUID(), Role.TEACHER);
    private final Actor otherTeacher = new Actor(UUID.randomUUID(), Role.TEACHER);
    private final Actor student = new Actor(UUID.randomUUID(), Role.STUDENT);
    private final Map<UUID, Boolean> revoked = new HashMap<>();
    private final List<AuditEvent> audits = new ArrayList<>();
    private final List<AiTaskRequest> submitted = new ArrayList<>();
    private final Map<UUID, AiTaskView> tasks = new HashMap<>();
    private Function<AiTaskRequest, AiTaskView> aiAnswer;
    private InMemoryQuizStore store;
    private FakeReferenceMaterial u13;
    private QuizAuthoringService authoring;
    private QuizPublicationService publication;
    private QuizAiDraftService drafts;
    private QuizDraftResultHandler handler;
    private QuizMcpTools mcp;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        store = new InMemoryQuizStore();
        u13 = new FakeReferenceMaterial();
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        IAuthorization authorization = (actor, action, ref) -> {
            boolean allowed = !revoked.containsKey(actor.accountId()) && (ref.scope() == ScopeKind.SUBJECT
                    ? actor.equals(manager) && subject.equals(ref.id())
                    : actor.equals(teacher) && classA.equals(ref.id()));
            return allowed ? AuthorizationDecision.allow() : AuthorizationDecision.deny("OUT_OF_SCOPE");
        };
        IClassAccess classes = mock(IClassAccess.class);
        when(classes.getClassRef(any())).thenAnswer(call -> classA.equals(call.getArgument(0))
                ? Optional.of(new ClassRef(classA, subject, "SE1801", "Lớp", ClassStatus.OPEN, teacher.accountId(),
                false))
                : Optional.empty());
        ISubjectQuery subjects = mock(ISubjectQuery.class);
        when(subjects.isActive(subject)).thenReturn(true);
        ObjectProvider<IQuizLifecycleListener> listeners = mock(ObjectProvider.class);
        when(listeners.orderedStream()).thenAnswer(call -> Stream.empty());
        aiAnswer = request -> new AiTaskView(UUID.randomUUID(), AiTaskState.QUEUED, request.source(),
                request.provider(), null, null, null, null);
        IAiTask ai = new IAiTask() {
            @Override
            public AiTaskView submit(AiTaskRequest request) {
                submitted.add(request);
                AiTaskView view = aiAnswer.apply(request);
                tasks.put(view.taskId(), view);
                return view;
            }

            @Override
            public Optional<AiTaskView> get(UUID taskId) {
                return Optional.ofNullable(tasks.get(taskId));
            }

            @Override
            public void cancel(Actor actor, UUID taskId) {
            }
        };

        QuizAudit audit = new QuizAudit(audits::add, clock);
        QuizAccess access = new QuizAccess(authorization, audit);
        ActiveSubjects active = new ActiveSubjects(subjects);
        QuizSupport support = new QuizSupport(store.quizRepository, store.questionRepository, classes, access, active);
        QuizReferenceMaterials materials = new QuizReferenceMaterials(u13, support, access);
        authoring = new QuizAuthoringService(store.quizRepository, store.questionRepository, support, access, audit,
                materials, clock);
        publication = new QuizPublicationService(store.quizRepository, support, listeners, event -> { }, audit, clock);
        drafts = new QuizAiDraftService(store.draftRepository, store.quizRepository, store.questionRepository, support,
                access, active, materials, ai, audit, TransactionOperations.withoutTransaction(), clock);
        handler = new QuizDraftResultHandler(store.draftRepository, store.quizRepository, support, access, active,
                new ObjectMapper(), clock);
        mcp = new QuizMcpTools(drafts, new ObjectMapper());
    }

    private static DraftRequest request(String ref, List<UUID> materials) {
        return new DraftRequest("Ôn tập chương 1", 2, null, Difficulty.EASY, materials, AiSource.SHARED, null, ref);
    }

    private static ApiException api(Runnable call) {
        try {
            call.run();
        } catch (ApiException failure) {
            return failure;
        }
        throw new AssertionError("Không ném ApiException");
    }

    private static QuestionContent valid(String stem) {
        return QuestionContent.of(QuestionType.MCQ_SINGLE, stem, List.of(new QuestionOption("1", "Đúng"),
                new QuestionOption("2", "Sai")), List.of("1"), null, new BigDecimal("2"), Difficulty.MEDIUM, List.of());
    }

    private QuizDetail classQuiz() {
        return authoring.createForClass(teacher, classA, "Luyện tập");
    }

    private AiDraftView readyQuizDraft(QuizDetail quiz) {
        AiDraftView pending = drafts.requestForQuiz(teacher, quiz.quiz().id(), request(UUID.randomUUID().toString(),
                List.of()), quiz.quiz().version());
        AiTaskRequest sent = submitted.get(submitted.size() - 1);
        assertThat(handler.onReady(new AiTaskResult(UUID.randomUUID(), sent.taskType(), teacher, sent.targetRef(),
                sent.inputVersion(), OUTPUT))).isEqualTo(ApplyOutcome.APPLIED);
        return drafts.getForQuiz(teacher, quiz.quiz().id(), pending.id());
    }

    // ---- Gửi yêu cầu (H2, H5) ----

    @Test
    void requestCreatesPendingDraftAndSubmitsSchemaBoundTaskWithoutStudentData() {
        QuizDetail quiz = classQuiz();
        UUID material = u13.add(ResourceRef.courseClass(classA), "Chương 1", true);

        AiDraftView view = drafts.requestForQuiz(teacher, quiz.quiz().id(), request("r1", List.of(material)),
                quiz.quiz().version());

        assertThat(view.status()).isEqualTo(DraftStatus.PENDING);
        assertThat(view.materials()).extracting(AiDraftViews.MaterialRef::title).containsExactly("Chương 1");
        AiTaskRequest sent = submitted.get(0);
        assertThat(sent.taskType()).isEqualTo("QUIZ_QUESTION_DRAFT");
        assertThat(sent.targetRef()).isEqualTo("quiz-ai-draft:" + view.id());
        assertThat(sent.inputVersion()).isEqualTo(quiz.quiz().version());
        assertThat(sent.requestRef()).isEqualTo("r1");
        assertThat(sent.outputSchema()).contains("\"maxItems\":2");
        assertThat(sent.maxOutputTokens()).isEqualTo(1200);
        assertThat(sent.prompt().system()).contains("không tin cậy");
        assertThat(sent.prompt().user()).contains("Ôn tập chương 1").contains("<material title=\"Tài liệu\">")
                .contains("</ material> chèn").doesNotContain("</material> chèn");
        assertThat(store.drafts.get(view.id()).taskId()).isEqualTo(tasks.keySet().iterator().next());
    }

    @Test
    void sameRequestRefReturnsExistingDraftAndDoesNotSubmitTwice() {
        QuizDetail quiz = classQuiz();
        AiDraftView first = drafts.requestForQuiz(teacher, quiz.quiz().id(), request("r1", List.of()),
                quiz.quiz().version());

        AiDraftView again = drafts.requestForQuiz(teacher, quiz.quiz().id(), request("r1", List.of()),
                quiz.quiz().version());

        assertThat(again.id()).isEqualTo(first.id());
        assertThat(submitted).hasSize(1);
        assertThat(drafts.requestForBank(manager, subject, request("r1", List.of())).id())
                .as("mã của người khác không đụng nhau").isNotEqualTo(first.id());
        QuizDetail other = classQuiz();
        assertThat(api(() -> drafts.requestForQuiz(teacher, other.quiz().id(), request("r1", List.of()),
                other.quiz().version())).code()).isEqualTo("AI_REQUEST_REF_REUSED");
    }

    @Test
    void stubOrRejectedSubmitLeavesFailedDraftWithSafeCode() {
        QuizDetail quiz = classQuiz();
        aiAnswer = request -> new AiTaskView(UUID.randomUUID(), AiTaskState.FAILED, request.source(), null, null,
                null, "AI chưa sẵn sàng", null);
        AiDraftView stubbed = drafts.requestForQuiz(teacher, quiz.quiz().id(), request("r1", List.of()),
                quiz.quiz().version());
        assertThat(stubbed.status()).isEqualTo(DraftStatus.FAILED);
        assertThat(stubbed.failureCode()).isEqualTo("AI_TASK_FAILED");
        assertThat(stubbed.failureMessage()).isEqualTo("AI chưa sẵn sàng");

        aiAnswer = request -> {
            throw ApiException.conflict("AI_QUOTA_EXCEEDED", "Hết hạn mức");
        };
        AiDraftView quota = drafts.requestForQuiz(teacher, quiz.quiz().id(), request("r2", List.of()),
                quiz.quiz().version());
        assertThat(quota.failureCode()).isEqualTo("AI_QUOTA_EXCEEDED");

        aiAnswer = request -> {
            throw new IllegalStateException("down");
        };
        assertThat(drafts.requestForQuiz(teacher, quiz.quiz().id(), request("r3", List.of()), quiz.quiz().version())
                .failureCode()).isEqualTo("AI_SERVICE_UNAVAILABLE");
        assertThat(drafts.listForQuiz(teacher, quiz.quiz().id())).hasSize(3);
    }

    @Test
    void invalidRequestsAreRejectedBeforeAnyDraft() {
        QuizDetail quiz = classQuiz();
        UUID id = quiz.quiz().id();
        long version = quiz.quiz().version();
        DraftRequest personalWithoutProvider = new DraftRequest("x", 2, null, null, List.of(), AiSource.PERSONAL,
                null, "r");
        DraftRequest sharedWithProvider = new DraftRequest("x", 2, null, null, List.of(), AiSource.SHARED,
                AiProvider.OPENAI, "r");
        DraftRequest tooMany = new DraftRequest("x", 51, null, null, List.of(), AiSource.SHARED, null, "r");
        DraftRequest blank = new DraftRequest("  ", 2, null, null, List.of(), AiSource.SHARED, null, "r");

        for (DraftRequest bad : List.of(personalWithoutProvider, sharedWithProvider, tooMany, blank)) {
            assertThat(api(() -> drafts.requestForQuiz(teacher, id, bad, version)).code())
                    .isEqualTo("AI_DRAFT_REQUEST_INVALID");
        }
        assertThat(api(() -> drafts.requestForQuiz(teacher, id, request("r", List.of()), version + 1)).code())
                .isEqualTo("QUIZ_VERSION_CONFLICT");
        assertThat(api(() -> drafts.requestForQuiz(otherTeacher, id, request("r", List.of()), version)).status())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(store.drafts).isEmpty();
        assertThat(submitted).isEmpty();
    }

    @Test
    void materialsMustBeSelectableWithTextAndU13FailureOnlyFailsTheDraft() {
        QuizDetail quiz = classQuiz();
        UUID noText = u13.add(ResourceRef.courseClass(classA), "Video", false);
        UUID otherScope = u13.add(ResourceRef.subject(UUID.randomUUID()), "Môn khác", true);
        UUID ok = u13.add(ResourceRef.courseClass(classA), "Chương 1", true);
        UUID id = quiz.quiz().id();
        long version = quiz.quiz().version();

        assertThat(api(() -> drafts.requestForQuiz(teacher, id, request("a", List.of(noText)), version)).code())
                .isEqualTo("MATERIAL_NOT_SELECTABLE");
        assertThat(api(() -> drafts.requestForQuiz(teacher, id, request("b", List.of(otherScope)), version)).code())
                .isEqualTo("MATERIAL_NOT_SELECTABLE");

        u13.down = true;
        assertThat(api(() -> drafts.requestForQuiz(teacher, id, request("c", List.of(ok)), version)).code())
                .isEqualTo("MATERIAL_SERVICE_UNAVAILABLE");
        assertThat(drafts.requestForQuiz(teacher, id, request("d", List.of()), version).status())
                .as("không chọn tài liệu thì U13 lỗi không ảnh hưởng").isEqualTo(DraftStatus.PENDING);
    }

    @Test
    void bankRequestNeedsSubjectManagerOfActiveSubject() {
        AiDraftView view = drafts.requestForBank(manager, subject, request("b1", List.of()));

        assertThat(view.target()).isEqualTo(DraftTarget.BANK);
        assertThat(submitted.get(0).inputVersion()).isZero();
        assertThat(submitted.get(0).prompt().user()).contains("ngân hàng câu hỏi");
        assertThat(api(() -> drafts.requestForBank(teacher, subject, request("b2", List.of()))).status())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(drafts.listForBank(manager, subject)).extracting(AiDraftView::id).containsExactly(view.id());
    }

    // ---- Nhận kết quả (H6) ----

    @Test
    void readyResultStoresProposalWithViolationsButAddsNoQuestion() {
        QuizDetail quiz = classQuiz();

        AiDraftView ready = readyQuizDraft(quiz);

        assertThat(ready.status()).isEqualTo(DraftStatus.READY);
        assertThat(ready.questions()).hasSize(2);
        DraftQuestion first = ready.questions().get(0);
        assertThat(first.content().correctOptionIds()).containsExactly("2");
        assertThat(first.content().defaultPoints()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(first.violations()).isEmpty();
        assertThat(ready.questions().get(1).violations()).extracting(v -> v.code())
                .contains("OPTION_DUPLICATE", "ANSWER_COUNT");
        assertThat(store.questions).isEmpty();
    }

    @Test
    void resultIsRejectedWhenQuizNoLongerEditableOrRequesterLostAccess() {
        QuizDetail quiz = classQuiz();
        drafts.requestForQuiz(teacher, quiz.quiz().id(), request("r1", List.of()), quiz.quiz().version());
        AiTaskRequest sent = submitted.get(0);
        Question q = Question.newBankDraft(subject, valid("Câu ngân hàng"), manager.accountId(), NOW).activate(NOW);
        store.put(q);
        QuizDetail withItem = authoring.saveItems(teacher, quiz.quiz().id(),
                List.of(new QuizAuthoringService.ItemInput(q.id(), null)), quiz.quiz().version());
        QuizDetail reviewed = publication.review(teacher, quiz.quiz().id(), withItem.quiz().version());
        publication.publish(teacher, quiz.quiz().id(), reviewed.quiz().version());

        ApplyOutcome outcome = handler.onReady(new AiTaskResult(UUID.randomUUID(), sent.taskType(), teacher,
                sent.targetRef(), sent.inputVersion(), OUTPUT));

        assertThat(outcome).isEqualTo(ApplyOutcome.REJECTED);
        AiDraftView failed = drafts.listForQuiz(teacher, quiz.quiz().id()).get(0);
        assertThat(failed.status()).isEqualTo(DraftStatus.FAILED);
        assertThat(failed.failureCode()).isEqualTo("QUIZ_NOT_EDITABLE");

        QuizDetail second = classQuiz();
        drafts.requestForQuiz(teacher, second.quiz().id(), request("r2", List.of()), second.quiz().version());
        AiTaskRequest secondSent = submitted.get(1);
        revoked.put(teacher.accountId(), true);
        assertThat(handler.onReady(new AiTaskResult(UUID.randomUUID(), secondSent.taskType(), teacher,
                secondSent.targetRef(), 0, OUTPUT))).isEqualTo(ApplyOutcome.REJECTED);
        assertThat(audits).extracting(AuditEvent::action).doesNotContain("ACCESS_DENIED");
    }

    @Test
    void discardedDraftIsStaleAndBrokenOutputOrFailureFailsPendingDraft() {
        QuizDetail quiz = classQuiz();
        AiDraftView pending = drafts.requestForQuiz(teacher, quiz.quiz().id(), request("r1", List.of()),
                quiz.quiz().version());
        drafts.discardForQuiz(teacher, quiz.quiz().id(), pending.id());
        AiTaskRequest sent = submitted.get(0);
        assertThat(handler.onReady(new AiTaskResult(UUID.randomUUID(), sent.taskType(), teacher, sent.targetRef(), 0,
                OUTPUT))).isEqualTo(ApplyOutcome.STALE);
        assertThat(handler.onReady(new AiTaskResult(UUID.randomUUID(), sent.taskType(), teacher, "quiz-ai-draft:x", 0,
                OUTPUT))).isEqualTo(ApplyOutcome.REJECTED);

        AiDraftView broken = drafts.requestForQuiz(teacher, quiz.quiz().id(), request("r2", List.of()),
                quiz.quiz().version());
        assertThat(handler.onReady(new AiTaskResult(UUID.randomUUID(), sent.taskType(), teacher,
                submitted.get(1).targetRef(), 0, "không phải JSON"))).isEqualTo(ApplyOutcome.REJECTED);
        assertThat(drafts.getForQuiz(teacher, quiz.quiz().id(), broken.id()).failureCode())
                .isEqualTo("AI_OUTPUT_INVALID");

        AiDraftView cancelled = drafts.requestForQuiz(teacher, quiz.quiz().id(), request("r3", List.of()),
                quiz.quiz().version());
        handler.onFailed(new AiTaskFailure(UUID.randomUUID(), sent.taskType(), teacher, submitted.get(2).targetRef(),
                AiTaskState.CANCELLED, "Đã hủy"));
        assertThat(drafts.getForQuiz(teacher, quiz.quiz().id(), cancelled.id()).failureCode())
                .isEqualTo("AI_TASK_CANCELLED");
    }

    @Test
    void viewingPendingDraftReconcilesTaskThatFailedBeforeCallback() {
        QuizDetail quiz = classQuiz();
        AiDraftView pending = drafts.requestForQuiz(teacher, quiz.quiz().id(), request("r1", List.of()),
                quiz.quiz().version());
        UUID taskId = store.drafts.get(pending.id()).taskId();
        tasks.put(taskId, new AiTaskView(taskId, AiTaskState.FAILED, AiSource.SHARED, null, null, null, "Lỗi nhà cung cấp",
                null));

        AiDraftView seen = drafts.getForQuiz(teacher, quiz.quiz().id(), pending.id());

        assertThat(seen.status()).isEqualTo(DraftStatus.FAILED);
        assertThat(seen.failureMessage()).isEqualTo("Lỗi nhà cung cấp");
    }

    // ---- Chấp nhận, bỏ (H4) ----

    @Test
    void acceptAppendsEditedQuestionsAsInlineOnceAndClosesDraft() {
        QuizDetail quiz = classQuiz();
        AiDraftView ready = readyQuizDraft(quiz);
        audits.clear();

        QuizDetail after = drafts.acceptForQuiz(teacher, quiz.quiz().id(), ready.id(),
                List.of(valid("Câu đã sửa"), ready.questions().get(0).content()), quiz.quiz().version());

        assertThat(after.quiz().version()).isEqualTo(quiz.quiz().version() + 1);
        assertThat(after.items()).hasSize(2).allSatisfy(item -> assertThat(item.inline()).isTrue());
        assertThat(after.items().get(0).question().stem()).isEqualTo("Câu đã sửa");
        assertThat(after.items().get(0).points()).isEqualByComparingTo("2");
        assertThat(store.drafts.get(ready.id()).status()).isEqualTo(DraftStatus.ACCEPTED);
        assertThat(audits).singleElement().satisfies(e -> {
            assertThat(e.action()).isEqualTo("QUIZ_AI_DRAFT_ACCEPTED");
            assertThat(e.details()).containsEntry("count", 2).doesNotContainKey("stem");
        });
        assertThat(api(() -> drafts.acceptForQuiz(teacher, quiz.quiz().id(), ready.id(), List.of(valid("x")),
                after.quiz().version())).code()).isEqualTo("AI_DRAFT_CLOSED");
    }

    @Test
    void acceptRejectsInvalidQuestionsStaleVersionAndOverflow() {
        QuizDetail quiz = classQuiz();
        AiDraftView ready = readyQuizDraft(quiz);
        UUID id = quiz.quiz().id();
        long version = quiz.quiz().version();

        ApiException invalid = api(() -> drafts.acceptForQuiz(teacher, id, ready.id(),
                List.of(ready.questions().get(1).content()), version));
        assertThat(invalid.status()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(invalid.code()).isEqualTo("AI_DRAFT_INVALID");
        assertThat(((RuleViolationException) invalid).violations()).allSatisfy(v ->
                assertThat(v.field()).startsWith("questions[0]."));
        assertThat(api(() -> drafts.acceptForQuiz(teacher, id, ready.id(), List.of(valid("a")), version + 1))
                .code()).isEqualTo("QUIZ_VERSION_CONFLICT");
        assertThat(api(() -> drafts.acceptForQuiz(teacher, id, ready.id(), List.of(valid("a"), valid("b"),
                valid("c")), version)).code()).isEqualTo("AI_DRAFT_SELECTION_INVALID");

        List<QuizItem> full = new ArrayList<>();
        for (int i = 1; i <= QuizRules.MAX_ITEMS - 1; i++) {
            Question q = Question.newInline(id, subject, valid("Câu " + i), teacher.accountId(), NOW);
            store.put(q);
            full.add(new QuizItem(id, q.id(), i, BigDecimal.ONE));
        }
        store.quizRepository.replaceItems(id, full);
        assertThat(api(() -> drafts.acceptForQuiz(teacher, id, ready.id(), List.of(valid("a"), valid("b")), version))
                .code()).isEqualTo("QUIZ_FULL");
        assertThat(store.drafts.get(ready.id()).status()).isEqualTo(DraftStatus.READY);
    }

    @Test
    void bankAcceptCreatesDraftQuestionsNeverActive() {
        AiDraftView pending = drafts.requestForBank(manager, subject, request("b1", List.of()));
        AiTaskRequest sent = submitted.get(0);
        handler.onReady(new AiTaskResult(UUID.randomUUID(), sent.taskType(), manager, sent.targetRef(), 0, OUTPUT));

        BankAcceptResult result = drafts.acceptForBank(manager, subject, pending.id(),
                List.of(drafts.getForBank(manager, subject, pending.id()).questions().get(0).content()));

        assertThat(result.questionIds()).hasSize(1);
        Question created = store.questions.get(result.questionIds().get(0));
        assertThat(created.scope()).isEqualTo(QuestionScope.BANK);
        assertThat(created.status()).isEqualTo(QuestionStatus.DRAFT);
        assertThat(result.draft().status()).isEqualTo(DraftStatus.ACCEPTED);
        assertThat(audits).extracting(AuditEvent::action).contains("QUIZ_AI_DRAFT_ACCEPTED");
    }

    @Test
    void pendingDraftCannotBeAcceptedAndClosedDraftCannotBeDiscarded() {
        QuizDetail quiz = classQuiz();
        AiDraftView pending = drafts.requestForQuiz(teacher, quiz.quiz().id(), request("r1", List.of()),
                quiz.quiz().version());

        assertThat(api(() -> drafts.acceptForQuiz(teacher, quiz.quiz().id(), pending.id(), List.of(valid("a")),
                quiz.quiz().version())).code()).isEqualTo("AI_DRAFT_NOT_READY");
        assertThat(drafts.discardForQuiz(teacher, quiz.quiz().id(), pending.id()).status())
                .isEqualTo(DraftStatus.DISCARDED);
        assertThat(api(() -> drafts.discardForQuiz(teacher, quiz.quiz().id(), pending.id())).code())
                .isEqualTo("AI_DRAFT_CLOSED");
        assertThat(drafts.listForQuiz(teacher, quiz.quiz().id())).isEmpty();
        assertThat(api(() -> drafts.getForBank(manager, subject, pending.id())).code()).isEqualTo("AI_DRAFT_NOT_FOUND");
    }

    // ---- Tài liệu gắn Quiz (H8) ----

    @Test
    void attachingMaterialsChecksOnlyNewlyAddedIds() {
        QuizDetail quiz = classQuiz();
        UUID ok = u13.add(ResourceRef.courseClass(classA), "Chương 1", false);
        UUID foreign = u13.add(ResourceRef.subject(UUID.randomUUID()), "Môn khác", true);
        UUID id = quiz.quiz().id();

        assertThat(api(() -> authoring.updateDetails(teacher, id, details(List.of(foreign)), quiz.quiz().version()))
                .code()).isEqualTo("MATERIAL_NOT_SELECTABLE");
        QuizDetail attached = authoring.updateDetails(teacher, id, details(List.of(ok)), quiz.quiz().version());
        assertThat(attached.quiz().referenceMaterialIds()).containsExactly(ok);

        u13.down = true;
        QuizDetail renamed = authoring.updateDetails(teacher, id, details(List.of(ok)), attached.quiz().version());
        assertThat(renamed.quiz().version()).isGreaterThan(attached.quiz().version());
        assertThat(api(() -> authoring.updateDetails(teacher, id, details(List.of(ok, UUID.randomUUID())),
                renamed.quiz().version())).code()).isEqualTo("MATERIAL_SERVICE_UNAVAILABLE");
    }

    private static Details details(List<UUID> materials) {
        return new Details("Luyện tập", null, Quiz.DEFAULT_SETTINGS, false, null,
                materials);
    }

    // ---- MCP (H7, BR-U10-42) ----

    @Test
    void mcpToolsAreForEducatorsWithQuizAccessOnly() {
        QuizDetail quiz = classQuiz();
        Map<String, Object> read = Map.of("quizId", quiz.quiz().id().toString());

        assertThat(mcp.tools()).extracting(t -> t.name()).containsExactly("read_quiz_context", "save_quiz_draft");
        assertThat(mcp.tools()).allSatisfy(t -> assertThat(t.allowedRoles()).doesNotContain(Role.STUDENT));
        assertThat(api(() -> mcp.call(student, "read_quiz_context", read)).status()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(api(() -> mcp.call(otherTeacher, "read_quiz_context", read)).status())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(api(() -> mcp.call(teacher, "read_quiz_context", Map.of("quizId", quiz.quiz().id().toString(),
                "extra", 1))).code()).isEqualTo("INVALID_MCP_ARGUMENTS");
        McpToolResult context = mcp.call(teacher, "read_quiz_context", read);
        assertThat(context.content()).isInstanceOf(QuizDetail.class);
    }

    @Test
    void saveQuizDraftStoresReadyMcpProposalForDraftQuizWithMatchingVersion() {
        QuizDetail quiz = classQuiz();
        List<Map<String, Object>> questions = List.of(Map.of("type", "MCQ_MULTI", "stem", "Số nguyên tố",
                "options", List.of("2", "3", "4"), "correct", List.of(1, 2), "points", 3));

        McpToolResult saved = mcp.call(teacher, "save_quiz_draft", Map.of("quizId", quiz.quiz().id().toString(),
                "expectedVersion", quiz.quiz().version(), "questions", questions, "agentLabel", "Claude Desktop"));

        assertThat(saved.error()).isFalse();
        AiDraftView draft = drafts.listForQuiz(teacher, quiz.quiz().id()).get(0);
        assertThat(draft.origin()).isEqualTo(DraftOrigin.MCP);
        assertThat(draft.status()).isEqualTo(DraftStatus.READY);
        assertThat(draft.agentLabel()).isEqualTo("Claude Desktop");
        assertThat(draft.questions().get(0).content().defaultPoints()).isEqualByComparingTo("3");
        assertThat(store.questions).as("chỉ là đề xuất").isEmpty();

        assertThat(api(() -> mcp.call(teacher, "save_quiz_draft", Map.of("quizId", quiz.quiz().id().toString(),
                "expectedVersion", quiz.quiz().version() + 1, "questions", questions))).code())
                .isEqualTo("QUIZ_VERSION_CONFLICT");
        assertThat(api(() -> mcp.call(teacher, "save_quiz_draft", Map.of("quizId", quiz.quiz().id().toString(),
                "expectedVersion", quiz.quiz().version(), "questions", List.of()))).code())
                .isEqualTo("INVALID_MCP_ARGUMENTS");
        assertThat(api(() -> mcp.call(teacher, "drop_quiz", Map.of())).code()).isEqualTo("MCP_TOOL_NOT_FOUND");
    }
}
