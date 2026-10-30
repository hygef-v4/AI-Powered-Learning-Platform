package edu.aiplatform.quizzes.service;

import com.fasterxml.jackson.databind.JsonNode;
import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.ai.contract.AiSource;
import edu.aiplatform.ai.contract.AiTaskRequest;
import edu.aiplatform.ai.contract.AiTaskState;
import edu.aiplatform.ai.contract.AiTaskView;
import edu.aiplatform.ai.contract.IAiTask;
import edu.aiplatform.ai.contract.MaterialText;
import edu.aiplatform.ai.contract.SelectableMaterial;
import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.ResourceRef;
import edu.aiplatform.quizzes.service.AiDraftViews.AiDraftView;
import edu.aiplatform.quizzes.service.AiDraftViews.BankAcceptResult;
import edu.aiplatform.quizzes.service.QuizViews.QuizDetail;
import edu.aiplatform.quizzes.contract.QuestionType;
import edu.aiplatform.quizzes.contract.QuizStatus;
import edu.aiplatform.quizzes.model.Difficulty;
import edu.aiplatform.quizzes.model.DraftQuestion;
import edu.aiplatform.quizzes.model.DraftStatus;
import edu.aiplatform.quizzes.model.DraftTarget;
import edu.aiplatform.quizzes.repository.IQuestionRepository;
import edu.aiplatform.quizzes.repository.IQuizAiDraftRepository;
import edu.aiplatform.quizzes.repository.IQuizRepository;
import edu.aiplatform.quizzes.model.Question;
import edu.aiplatform.quizzes.model.QuestionContent;
import edu.aiplatform.quizzes.model.QuestionRules;
import edu.aiplatform.quizzes.model.Quiz;
import edu.aiplatform.quizzes.model.QuizAiDraft;
import edu.aiplatform.quizzes.model.QuizItem;
import edu.aiplatform.quizzes.model.QuizRules;
import edu.aiplatform.quizzes.model.Violation;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionOperations;

/**
 * AI soạn câu cho ngân hàng (F4) và cho Quiz (F8), đề xuất từ MCP (F13): mọi câu vào {@code quiz_ai_drafts} trước,
 * chỉ lệnh chấp nhận mới thêm câu (P4, BR-U10-41). Gửi task hai bước (H2): ghi đề xuất {@code PENDING} trong
 * transaction, gọi U06 ngoài transaction, rồi ghi {@code taskId}.
 */
@Service
public class QuizAiDraftService {

    static final String TARGET_PREFIX = "quiz-ai-draft:";
    static final String OBJECT = "QUIZ_AI_DRAFT";
    static final String NOT_FOUND = "AI_DRAFT_NOT_FOUND";
    private static final int MAX_REQUEST_REF = 100;
    private static final Set<AiTaskState> TERMINAL_FAILURES = Set.of(AiTaskState.FAILED, AiTaskState.CANCELLED);

    private final IQuizAiDraftRepository drafts;
    private final IQuizRepository quizzes;
    private final IQuestionRepository questions;
    private final QuizSupport support;
    private final QuizAccess access;
    private final ActiveSubjects activeSubjects;
    private final QuizReferenceMaterials materials;
    private final IAiTask ai;
    private final QuizAudit audit;
    private final TransactionOperations transactions;
    private final Clock clock;

    public QuizAiDraftService(IQuizAiDraftRepository drafts, IQuizRepository quizzes, IQuestionRepository questions,
                              QuizSupport support, QuizAccess access, ActiveSubjects activeSubjects,
                              QuizReferenceMaterials materials, IAiTask ai, QuizAudit audit,
                              TransactionOperations transactions, Clock clock) {
        this.drafts = drafts;
        this.quizzes = quizzes;
        this.questions = questions;
        this.support = support;
        this.access = access;
        this.activeSubjects = activeSubjects;
        this.materials = materials;
        this.ai = ai;
        this.audit = audit;
        this.transactions = transactions;
        this.clock = clock;
    }

    /**
     * Yêu cầu AI (BR-U10-40): số câu 1–50, loại và độ khó tùy chọn, tối đa 10 tài liệu; nguồn AI chọn mỗi lần
     * ({@code provider} chỉ khi {@code PERSONAL}); {@code requestRef} chống gửi trùng.
     */
    public record DraftRequest(String instructions, int count, QuestionType type, Difficulty difficulty,
                               List<UUID> materialIds, AiSource source, AiProvider provider, String requestRef) {

        public DraftRequest {
            materialIds = materialIds == null ? List.of() : List.copyOf(materialIds);
            instructions = instructions == null ? null : instructions.strip();
        }
    }

    private record Prepared(QuizAiDraft draft, String target, boolean dispatch) {
    }

    // ---- Gửi yêu cầu ----

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AiDraftView requestForQuiz(Actor actor, UUID quizId, DraftRequest request, long version) {
        Prepared prepared = transactions.execute(status -> {
            Quiz quiz = support.loadEditable(actor, quizId, version);
            validate(request);
            Optional<QuizAiDraft> existing = existing(actor, request, DraftTarget.QUIZ, quizId);
            if (existing.isPresent()) {
                return new Prepared(existing.get(), null, false);
            }
            List<SelectableMaterial> chosen = materials.requireSelectable(actor, QuizReferenceMaterials.scopeOf(quiz),
                    request.materialIds(), true);
            QuizAiDraft draft = QuizAiDraft.pending(DraftTarget.QUIZ, quizId, quiz.subjectId(), quiz.version(),
                    provenance(request, chosen), actor.accountId(), clock.instant());
            drafts.insert(draft);
            return new Prepared(draft, "quiz \"" + quiz.title() + "\"", true);
        });
        return dispatch(actor, prepared, request);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AiDraftView requestForBank(Actor actor, UUID subjectId, DraftRequest request) {
        Prepared prepared = transactions.execute(status -> {
            access.requireSubject(actor, Actions.QUESTION_BANK_MANAGE, subjectId, "SUBJECT", subjectId,
                    "SUBJECT_NOT_FOUND");
            activeSubjects.require(subjectId);
            validate(request);
            Optional<QuizAiDraft> existing = existing(actor, request, DraftTarget.BANK, subjectId);
            if (existing.isPresent()) {
                return new Prepared(existing.get(), null, false);
            }
            List<SelectableMaterial> chosen = materials.requireSelectable(actor, ResourceRef.subject(subjectId),
                    request.materialIds(), true);
            QuizAiDraft draft = QuizAiDraft.pending(DraftTarget.BANK, null, subjectId, null,
                    provenance(request, chosen), actor.accountId(), clock.instant());
            drafts.insert(draft);
            return new Prepared(draft, "ngân hàng câu hỏi của môn", true);
        });
        return dispatch(actor, prepared, request);
    }

    private AiDraftView dispatch(Actor actor, Prepared prepared, DraftRequest request) {
        QuizAiDraft draft = prepared.draft();
        if (!prepared.dispatch()) {
            return AiDraftView.of(draft);
        }
        List<MaterialText> texts;
        try {
            texts = materials.texts(actor, request.materialIds());
        } catch (ApiException failure) {
            return fail(draft, failure.code(), failure.getMessage());
        }
        AiTaskView task;
        try {
            task = ai.submit(new AiTaskRequest(actor, request.source(), request.provider(), QuizDraftPrompt.TASK_TYPE,
                    TARGET_PREFIX + draft.id(), draft.baseVersion() == null ? 0 : draft.baseVersion(),
                    request.requestRef(), QuizDraftPrompt.prompt(prepared.target(), request, texts),
                    QuizDraftPrompt.schema(request.count()), QuizDraftPrompt.maxOutputTokens(request.count())));
        } catch (ApiException failure) {
            return fail(draft, failure.code(), failure.getMessage());
        } catch (RuntimeException failure) {
            return fail(draft, "AI_SERVICE_UNAVAILABLE", "Chưa gửi được yêu cầu AI, vui lòng thử lại");
        }
        return transactions.execute(status -> {
            if (task.taskId() != null) {
                drafts.attachTask(draft.id(), task.taskId());
            }
            QuizAiDraft current = drafts.find(draft.id()).orElseThrow(() -> ApiException.notFound(NOT_FOUND));
            if (task.state() != null && TERMINAL_FAILURES.contains(task.state())) {
                current = failIfPending(current, failureCode(task.state()), task.safeError());
            }
            return AiDraftView.of(current);
        });
    }

    private AiDraftView fail(QuizAiDraft draft, String code, String message) {
        return transactions.execute(status -> AiDraftView.of(failIfPending(
                drafts.find(draft.id()).orElseThrow(() -> ApiException.notFound(NOT_FOUND)), code, message)));
    }

    // ---- Xem ----

    @Transactional
    public List<AiDraftView> listForQuiz(Actor actor, UUID quizId) {
        support.loadManaged(actor, quizId);
        return drafts.listOpenForQuiz(quizId).stream().map(this::reconcile).map(AiDraftView::of).toList();
    }

    @Transactional
    public List<AiDraftView> listForBank(Actor actor, UUID subjectId) {
        requireBankManager(actor, subjectId);
        return drafts.listOpenForBank(subjectId).stream().map(this::reconcile).map(AiDraftView::of).toList();
    }

    @Transactional
    public AiDraftView getForQuiz(Actor actor, UUID quizId, UUID draftId) {
        support.loadManaged(actor, quizId);
        return AiDraftView.of(reconcile(quizDraft(quizId, draftId)));
    }

    @Transactional
    public AiDraftView getForBank(Actor actor, UUID subjectId, UUID draftId) {
        requireBankManager(actor, subjectId);
        return AiDraftView.of(reconcile(bankDraft(subjectId, draftId)));
    }

    /** Kết quả lỗi có thể về trước khi ghi {@code taskId}; hỏi lại U06 khi xem (như U09). */
    private QuizAiDraft reconcile(QuizAiDraft draft) {
        if (draft.status() != DraftStatus.PENDING || draft.taskId() == null) {
            return draft;
        }
        Optional<AiTaskView> task;
        try {
            task = ai.get(draft.taskId());
        } catch (RuntimeException unavailable) {
            return draft;
        }
        if (task.isPresent() && task.get().state() != null && TERMINAL_FAILURES.contains(task.get().state())) {
            return failIfPending(draft, failureCode(task.get().state()), task.get().safeError());
        }
        return draft;
    }

    // ---- Chấp nhận, bỏ ----

    /** Câu chọn (có thể đã sửa) thành câu riêng ở cuối Quiz; {@code version} của Quiz +1 một lần (H4). */
    @Transactional
    public QuizDetail acceptForQuiz(Actor actor, UUID quizId, UUID draftId, List<QuestionContent> picked,
                                    long version) {
        Quiz quiz = support.loadEditable(actor, quizId, version);
        QuizAiDraft draft = requireReady(quizDraft(quizId, draftId));
        validatePicked(picked, draft);
        List<QuizItem> items = new ArrayList<>(quizzes.items(quizId));
        if (items.size() + picked.size() > QuizRules.MAX_ITEMS) {
            throw ApiException.conflict("QUIZ_FULL", "Quiz tối đa 200 câu; còn " + (QuizRules.MAX_ITEMS - items.size())
                    + " chỗ trống");
        }
        for (QuestionContent content : picked) {
            Question inline = Question.newInline(quizId, quiz.subjectId(), content, actor.accountId(), clock.instant());
            questions.insert(inline);
            items.add(new QuizItem(quizId, inline.id(), items.size() + 1, content.defaultPoints()));
        }
        quizzes.replaceItems(quizId, items);
        QuizDetail detail = support.detail(support.save(quiz.edited(clock.instant())), true);
        close(draft, DraftStatus.ACCEPTED);
        audit.success(actor, "QUIZ_AI_DRAFT_ACCEPTED", QuizSupport.OBJECT, quizId, Map.of("draftId",
                draft.id().toString(), "origin", draft.origin().name(), "count", picked.size()));
        return detail;
    }

    /** Câu chọn thành câu {@code DRAFT} của ngân hàng; AI không tự kích hoạt (BR-U10-41). */
    @Transactional
    public BankAcceptResult acceptForBank(Actor actor, UUID subjectId, UUID draftId, List<QuestionContent> picked) {
        requireBankManager(actor, subjectId);
        activeSubjects.require(subjectId);
        QuizAiDraft draft = requireReady(bankDraft(subjectId, draftId));
        validatePicked(picked, draft);
        List<UUID> created = new ArrayList<>();
        for (QuestionContent content : picked) {
            Question q = Question.newBankDraft(subjectId, content, actor.accountId(), clock.instant());
            questions.insert(q);
            created.add(q.id());
        }
        QuizAiDraft closed = close(draft, DraftStatus.ACCEPTED);
        audit.success(actor, "QUIZ_AI_DRAFT_ACCEPTED", OBJECT, draft.id(), Map.of("subjectId", subjectId.toString(),
                "origin", draft.origin().name(), "count", picked.size()));
        return new BankAcceptResult(AiDraftView.of(closed), created);
    }

    @Transactional
    public AiDraftView discardForQuiz(Actor actor, UUID quizId, UUID draftId) {
        support.loadManaged(actor, quizId);
        return AiDraftView.of(discard(actor, quizDraft(quizId, draftId)));
    }

    @Transactional
    public AiDraftView discardForBank(Actor actor, UUID subjectId, UUID draftId) {
        requireBankManager(actor, subjectId);
        return AiDraftView.of(discard(actor, bankDraft(subjectId, draftId)));
    }

    private QuizAiDraft discard(Actor actor, QuizAiDraft draft) {
        if (!draft.status().isOpen()) {
            throw closedConflict();
        }
        if (draft.status() == DraftStatus.PENDING && draft.taskId() != null) {
            try {
                ai.cancel(actor, draft.taskId());
            } catch (RuntimeException ignored) {
                // Hủy task chỉ để đỡ tốn quota; đề xuất vẫn bỏ được khi U06 lỗi (BR-U10-52).
            }
        }
        return close(draft, DraftStatus.DISCARDED);
    }

    // ---- MCP (F13) ----

    /** {@code read_quiz_context}: Quiz kèm câu, đáp án, giải thích, {@code version}. */
    @Transactional(readOnly = true)
    public QuizDetail mcpContext(Actor actor, UUID quizId) {
        return support.detail(support.loadManaged(actor, quizId), true);
    }

    /** {@code save_quiz_draft}: Quiz {@code DRAFT}, {@code expectedVersion} khớp; lưu đề xuất {@code READY}. */
    @Transactional
    public AiDraftView saveFromMcp(Actor actor, UUID quizId, long expectedVersion, JsonNode questionNodes,
                                  String agentLabel) {
        Quiz quiz = support.loadManaged(actor, quizId);
        if (quiz.status() != QuizStatus.DRAFT) {
            throw ApiException.conflict("QUIZ_NOT_EDITABLE", "Agent chỉ ghi đề xuất cho quiz đang nháp");
        }
        QuizSupport.requireVersion(quiz, expectedVersion);
        support.requireContainerActive(quiz);
        List<DraftQuestion> proposal;
        try {
            proposal = QuizDraftPrompt.parseQuestions(questionNodes);
        } catch (IllegalArgumentException invalid) {
            throw ApiException.badRequest("INVALID_MCP_ARGUMENTS", invalid.getMessage());
        }
        Map<String, Object> provenance = new LinkedHashMap<>();
        provenance.put(AiDraftViews.AGENT_LABEL, agentLabel == null || agentLabel.isBlank() ? "Agent" : agentLabel);
        provenance.put(AiDraftViews.REQUESTED_COUNT, proposal.size());
        QuizAiDraft draft = QuizAiDraft.fromMcp(quizId, quiz.subjectId(), expectedVersion, proposal, provenance,
                actor.accountId(), clock.instant());
        drafts.insert(draft);
        return AiDraftView.of(draft);
    }

    /** Bank proposals share the same READY review/acceptance flow as provider-generated proposals. */
    @Transactional
    public AiDraftView saveBankFromMcp(Actor actor, UUID subjectId, JsonNode questionNodes, String agentLabel) {
        requireBankManager(actor, subjectId);
        activeSubjects.require(subjectId);
        List<DraftQuestion> proposal;
        try { proposal = QuizDraftPrompt.parseQuestions(questionNodes); }
        catch (IllegalArgumentException invalid) { throw ApiException.badRequest("INVALID_MCP_ARGUMENTS", "Invalid questions"); }
        if(agentLabel != null && agentLabel.length()>200)throw ApiException.badRequest("INVALID_MCP_ARGUMENTS", "Invalid agent label");
        var draft = new QuizAiDraft(UUID.randomUUID(), DraftTarget.BANK, null, subjectId,
                edu.aiplatform.quizzes.model.DraftOrigin.MCP, null, DraftStatus.READY, proposal,
                Map.of(AiDraftViews.AGENT_LABEL, agentLabel==null||agentLabel.isBlank()?"Agent":agentLabel,
                        AiDraftViews.REQUESTED_COUNT, proposal.size()), null, actor.accountId(), clock.instant(), null);
        drafts.insert(draft);
        return AiDraftView.of(draft);
    }

    // ---- Phụ ----

    private void requireBankManager(Actor actor, UUID subjectId) {
        access.requireSubject(actor, Actions.QUESTION_BANK_MANAGE, subjectId, "SUBJECT", subjectId,
                "SUBJECT_NOT_FOUND");
    }

    private QuizAiDraft quizDraft(UUID quizId, UUID draftId) {
        return drafts.find(draftId)
                .filter(d -> d.target() == DraftTarget.QUIZ && quizId.equals(d.quizId()))
                .orElseThrow(() -> ApiException.notFound(NOT_FOUND));
    }

    private QuizAiDraft bankDraft(UUID subjectId, UUID draftId) {
        return drafts.find(draftId)
                .filter(d -> d.target() == DraftTarget.BANK && subjectId.equals(d.subjectId()))
                .orElseThrow(() -> ApiException.notFound(NOT_FOUND));
    }

    private static QuizAiDraft requireReady(QuizAiDraft draft) {
        if (draft.status() != DraftStatus.READY) {
            throw draft.status() == DraftStatus.PENDING
                    ? ApiException.conflict("AI_DRAFT_NOT_READY", "Đề xuất AI chưa xong")
                    : closedConflict();
        }
        return draft;
    }

    private QuizAiDraft close(QuizAiDraft draft, DraftStatus decision) {
        QuizAiDraft closed = draft.decided(decision, clock.instant());
        if (!drafts.transition(closed, draft.status())) {
            throw closedConflict();
        }
        return closed;
    }

    private QuizAiDraft failIfPending(QuizAiDraft draft, String code, String message) {
        if (draft.status() != DraftStatus.PENDING) {
            return draft;
        }
        QuizAiDraft failed = draft.failed(code, message, clock.instant());
        return drafts.transition(failed, DraftStatus.PENDING) ? failed
                : drafts.find(draft.id()).orElse(draft);
    }

    private static ApiException closedConflict() {
        return ApiException.conflict("AI_DRAFT_CLOSED", "Đề xuất đã được chấp nhận hoặc bỏ; hãy tải lại");
    }

    static String failureCode(AiTaskState state) {
        return state == AiTaskState.CANCELLED ? "AI_TASK_CANCELLED" : "AI_TASK_FAILED";
    }

    /** Câu chọn: 1 tới số câu đề xuất; mỗi câu theo BR-U10-02, 03; lỗi trả theo {@code questions[i]}. */
    private static void validatePicked(List<QuestionContent> picked, QuizAiDraft draft) {
        if (picked == null || picked.isEmpty() || picked.size() > draft.proposal().size()) {
            throw ApiException.badRequest("AI_DRAFT_SELECTION_INVALID",
                    "Chọn từ 1 đến " + draft.proposal().size() + " câu để chấp nhận");
        }
        List<Violation> violations = new ArrayList<>();
        for (int i = 0; i < picked.size(); i++) {
            for (Violation v : QuestionRules.validate(picked.get(i))) {
                violations.add(new Violation("questions[" + i + "]." + v.field(), v.code(), v.message()));
            }
        }
        if (!violations.isEmpty()) {
            throw new RuleViolationException(HttpStatus.UNPROCESSABLE_ENTITY, "AI_DRAFT_INVALID",
                    "Có câu chưa hợp lệ, hãy sửa trước khi chấp nhận", violations);
        }
    }

    private static void validate(DraftRequest r) {
        if (r.instructions() == null || r.instructions().isEmpty()
                || r.instructions().codePointCount(0, r.instructions().length()) > QuizDraftPrompt.MAX_INSTRUCTIONS) {
            throw invalidRequest("Nhập yêu cầu, tối đa 2.000 ký tự");
        }
        if (r.count() < 1 || r.count() > QuizDraftPrompt.MAX_COUNT) {
            throw invalidRequest("Số câu từ 1 đến 50");
        }
        if (r.materialIds().size() > QuizDraftPrompt.MAX_MATERIALS
                || new HashSet<>(r.materialIds()).size() != r.materialIds().size()) {
            throw invalidRequest("Chọn tối đa 10 tài liệu, không trùng");
        }
        if (r.source() == null || (r.source() == AiSource.PERSONAL) != (r.provider() != null)) {
            throw invalidRequest("Chọn nguồn AI; API cá nhân cần chọn nhà cung cấp");
        }
        if (r.requestRef() == null || r.requestRef().isBlank() || r.requestRef().length() > MAX_REQUEST_REF) {
            throw invalidRequest("Mã yêu cầu không hợp lệ");
        }
    }

    private static ApiException invalidRequest(String message) {
        return ApiException.badRequest("AI_DRAFT_REQUEST_INVALID", message);
    }

    /** Cùng {@code requestRef} cho đích khác thì từ chối, cho cùng đích thì trả đề xuất cũ. */
    private Optional<QuizAiDraft> existing(Actor actor, DraftRequest request, DraftTarget target, UUID targetId) {
        Optional<QuizAiDraft> found = drafts.findByRequest(actor.accountId(), request.requestRef());
        found.ifPresent(d -> {
            UUID id = target == DraftTarget.QUIZ ? d.quizId() : d.subjectId();
            if (d.target() != target || !targetId.equals(id)) {
                throw ApiException.conflict("AI_REQUEST_REF_REUSED", "Mã yêu cầu đã dùng cho đích khác");
            }
        });
        return found;
    }

    private static Map<String, Object> provenance(DraftRequest request, List<SelectableMaterial> chosen) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put(QuizAiDraft.REQUEST_REF, request.requestRef());
        p.put(AiDraftViews.SOURCE, request.source().name());
        if (request.provider() != null) {
            p.put(AiDraftViews.PROVIDER, request.provider().name());
        }
        p.put(AiDraftViews.REQUESTED_COUNT, request.count());
        p.put(AiDraftViews.MATERIALS, chosen.stream()
                .map(m -> Map.of("id", m.materialId().toString(), "title", m.title() == null ? "" : m.title()))
                .toList());
        return p;
    }
}
