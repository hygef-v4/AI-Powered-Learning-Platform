package edu.aiplatform.quizzes.service;

import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.ai.contract.AiSource;
import edu.aiplatform.ai.contract.AiTaskRequest;
import edu.aiplatform.ai.contract.AiTaskState;
import edu.aiplatform.ai.contract.IAiTask;
import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.ResourceRef;
import edu.aiplatform.quizzes.contract.QuestionType;
import edu.aiplatform.quizzes.model.Difficulty;
import edu.aiplatform.quizzes.model.DraftStatus;
import edu.aiplatform.quizzes.model.DraftTarget;
import edu.aiplatform.quizzes.model.QuizAiDraft;
import edu.aiplatform.quizzes.repository.IQuizAiDraftRepository;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionOperations;

/** Một lượt tạo lại bỏ prompt lệch chủ đề; khóa draft và gửi job trong cùng transaction. */
@Service
public class QuizSourceRetry {
    private final IQuizAiDraftRepository drafts;
    private final QuizSupport support;
    private final QuizAccess access;
    private final ActiveSubjects activeSubjects;
    private final QuizReferenceMaterials materials;
    private final IAiTask ai;
    private final TransactionOperations transactions;
    private final Clock clock;

    public QuizSourceRetry(IQuizAiDraftRepository drafts, QuizSupport support, QuizAccess access,
                           ActiveSubjects activeSubjects, QuizReferenceMaterials materials, IAiTask ai,
                           TransactionOperations transactions, Clock clock) {
        this.drafts = drafts;
        this.support = support;
        this.access = access;
        this.activeSubjects = activeSubjects;
        this.materials = materials;
        this.ai = ai;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public QuizAiDraft retry(Actor actor, UUID draftId) {
        try {
            return transactions.execute(status -> {
                QuizAiDraft draft = drafts.findForUpdate(draftId).orElseThrow(() -> ApiException.createNotFound("AI_DRAFT_NOT_FOUND"));
                if (!draft.needsSourceRetry() || actor == null || !draft.createdBy().equals(actor.accountId())) return draft;
                ResourceRef scope;
                if (draft.target() == DraftTarget.QUIZ) {
                    var quiz = support.loadEditable(actor, draft.quizId(), draft.baseVersion());
                    support.requireNoAttemptsWhenOpen(quiz);
                    scope = QuizReferenceMaterials.resolveScope(quiz);
                } else {
                    access.requireSubject(actor, Actions.QUESTION_BANK_MANAGE, draft.subjectId(), "SUBJECT",
                            draft.subjectId(), "SUBJECT_NOT_FOUND");
                    activeSubjects.require(draft.subjectId());
                    scope = ResourceRef.subject(draft.subjectId());
                }
                var ids = AiDraftViews.AiDraftView.createDraftView(draft).materials().stream().map(AiDraftViews.MaterialRef::id).toList();
                materials.requireSelectable(actor, scope, ids, true);
                var texts = materials.loadTexts(actor, ids);
                if (texts.stream().noneMatch(text -> !text.text().isBlank())) {
                    throw ApiException.createBadRequest("MATERIAL_TEXT_UNAVAILABLE", "Tài liệu đã chọn không còn nội dung dùng được.");
                }
                var p = draft.provenance();
                int count = ((Number) p.get(AiDraftViews.REQUESTED_COUNT)).intValue();
                var request = new QuizAiDraftService.DraftRequest(QuizDraftPrompt.DEFAULT_SOURCE_INSTRUCTIONS, count,
                        p.get("requestedType") instanceof String type ? QuestionType.valueOf(type) : null,
                        p.get("requestedDifficulty") instanceof String difficulty ? Difficulty.valueOf(difficulty) : null,
                        ids, p.get(AiDraftViews.SOURCE) instanceof String source && !source.equals("AUTO") ? AiSource.valueOf(source) : null,
                        p.get(AiDraftViews.PROVIDER) instanceof String provider ? AiProvider.valueOf(provider) : null,
                        "quiz-source-retry:" + draft.id());
                var task = ai.submit(new AiTaskRequest(actor, request.source(), request.provider(), QuizDraftPrompt.TASK_TYPE,
                        QuizAiDraftService.TARGET_PREFIX + draft.id(), draft.baseVersion() == null ? 0 : draft.baseVersion(),
                        request.requestRef(), QuizDraftPrompt.prompt("quiz theo tài liệu nguồn", request, texts),
                        QuizDraftPrompt.buildSchema(count), QuizDraftPrompt.calculateMaxOutputTokens(count),
                        QuizDraftPrompt.buildUserInput(actor, request)));
                if (task.taskId() == null || task.state() == AiTaskState.FAILED || task.state() == AiTaskState.CANCELLED) {
                    throw ApiException.createUnavailable("AI_SOURCE_RETRY_FAILED", "Không gửi được lượt tạo lại theo tài liệu.");
                }
                QuizAiDraft next = draft.attachSourceRetryTask(task.taskId());
                if (!drafts.transitionDraft(next, DraftStatus.PENDING)) {
                    throw ApiException.createConflict("AI_DRAFT_CLOSED", "Đề xuất đã thay đổi.");
                }
                return next;
            });
        } catch (RuntimeException failure) {
            // Chờ transaction submit rollback xong rồi mới lưu lỗi, không nuốt rollback-only của U06.
            return transactions.execute(status -> {
                QuizAiDraft current = drafts.findForUpdate(draftId).orElseThrow(() -> ApiException.createNotFound("AI_DRAFT_NOT_FOUND"));
                if (!current.needsSourceRetry() || actor == null || !current.createdBy().equals(actor.accountId())) return current;
                QuizAiDraft failed = current.markFailed(failure instanceof ApiException api ? api.readCode() : "AI_SOURCE_RETRY_FAILED",
                        "Không tạo lại được câu hỏi theo tài liệu. Kiểm tra nguồn hoặc gửi yêu cầu mới.", clock.instant());
                drafts.transitionDraft(failed, DraftStatus.PENDING);
                return failed;
            });
        }
    }
}
