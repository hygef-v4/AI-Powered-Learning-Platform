package edu.aiplatform.quizzes.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.ai.contract.AiTaskFailure;
import edu.aiplatform.ai.contract.AiTaskResult;
import edu.aiplatform.ai.contract.ApplyOutcome;
import edu.aiplatform.ai.contract.IAiTaskResultHandler;
import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.ResourceRef;
import edu.aiplatform.quizzes.model.DraftStatus;
import edu.aiplatform.quizzes.model.DraftTarget;
import edu.aiplatform.quizzes.repository.IQuizAiDraftRepository;
import edu.aiplatform.quizzes.repository.IQuizRepository;
import edu.aiplatform.quizzes.model.Quiz;
import edu.aiplatform.quizzes.model.QuizAiDraft;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Nhận kết quả task {@code QUIZ_QUESTION_DRAFT} trong transaction của U06 (H6): kiểm lại quyền người yêu cầu và
 * đích rồi lưu đề xuất {@code READY}; không thêm câu vào Quiz hay ngân hàng (P4).
 */
@Component
public class QuizDraftResultHandler implements IAiTaskResultHandler {
    private static final Logger log = LoggerFactory.getLogger(QuizDraftResultHandler.class);

    private final IQuizAiDraftRepository drafts;
    private final IQuizRepository quizzes;
    private final QuizSupport support;
    private final QuizAccess access;
    private final ActiveSubjects activeSubjects;
    private final ObjectMapper json;
    private final Clock clock;
    private final QuizSourceRetry sourceRetry;

    public QuizDraftResultHandler(IQuizAiDraftRepository drafts, IQuizRepository quizzes, QuizSupport support,
                                  QuizAccess access, ActiveSubjects activeSubjects, ObjectMapper json, Clock clock, QuizSourceRetry sourceRetry) {
        this.drafts = drafts;
        this.quizzes = quizzes;
        this.support = support;
        this.access = access;
        this.activeSubjects = activeSubjects;
        this.json = json;
        this.clock = clock;
        this.sourceRetry = sourceRetry;
    }

    @Override
    public String taskType() {
        return QuizDraftPrompt.TASK_TYPE;
    }

    @Override
    public ApplyOutcome onReady(AiTaskResult result) {
        Optional<QuizAiDraft> found = draftOf(result.targetRef());
        if (found.isEmpty()) {
            return ApplyOutcome.REJECTED;
        }
        QuizAiDraft draft = found.get();
        if (draft.status() != DraftStatus.PENDING) {
            return ApplyOutcome.STALE;
        }
        if (draft.sourceRetryAttempted() && !java.util.Objects.equals(draft.taskId(), result.taskId())) return ApplyOutcome.STALE;
        String problem = targetProblem(result.actor(), draft);
        if (problem != null) {
            fail(draft, problem, "Đích của đề xuất không còn nhận câu mới");
            return ApplyOutcome.REJECTED;
        }
        QuizDraftPrompt.Output output;
        try {
            output = QuizDraftPrompt.parseOutput(json, result.output());
        } catch (IllegalArgumentException invalid) {
            fail(draft, "AI_OUTPUT_INVALID", "Kết quả AI không đúng định dạng câu hỏi");
            return ApplyOutcome.REJECTED;
        }
        if ((!output.requestMatchesSources() || !output.questionsMatchSources())
                && draft.provenance().get(AiDraftViews.MATERIALS) instanceof List<?> materials
                && !materials.isEmpty()) {
            if (draft.sourceRetryAttempted()) {
                fail(draft, "AI_SOURCE_MISMATCH", "AI vẫn tạo câu hỏi không bám tài liệu. Đề xuất đã bị chặn; hãy kiểm tra tài liệu hoặc gửi yêu cầu mới.");
                return ApplyOutcome.REJECTED;
            }
            List<String> warnings = new ArrayList<>();
            if (draft.provenance().get("sourceWarnings") instanceof List<?> existing) {
                existing.stream().filter(String.class::isInstance).map(String.class::cast).forEach(warnings::add);
            }
            warnings.add("Yêu cầu hoặc câu hỏi AI tạo có thể không liên quan đến tài liệu đã chọn. Hệ thống bỏ prompt bổ sung và tạo lại một lần chỉ theo tài liệu nguồn.");
            if (!drafts.transition(draft.retryFromSources(warnings), DraftStatus.PENDING)) return ApplyOutcome.STALE;
            retryAfterCommit(result.actor(), draft.id());
            return ApplyOutcome.APPLIED;
        }
        if (!drafts.transition(draft.ready(output.questions(), Map.of()), DraftStatus.PENDING)) {
            return ApplyOutcome.STALE;
        }
        if (result.taskId() != null) {
            drafts.attachTask(draft.id(), result.taskId());
        }
        return ApplyOutcome.APPLIED;
    }

    @Override
    public void onFailed(AiTaskFailure failure) {
        draftOf(failure.targetRef())
                .filter(d -> d.status() == DraftStatus.PENDING)
                .filter(d -> !d.sourceRetryAttempted() || java.util.Objects.equals(d.taskId(), failure.taskId()))
                .ifPresent(d -> fail(d, QuizAiDraftService.failureCode(failure.state()), failure.safeError()));
    }

    private void retryAfterCommit(Actor actor, UUID draftId) {
        Runnable retry = () -> {
            try { sourceRetry.retry(actor, draftId); }
            catch (RuntimeException unavailable) {
                log.warn("Source-only retry deferred for draftId={}; owner polling can resume it", draftId, unavailable);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { retry.run(); }
            });
        } else retry.run();
    }

    /** {@code null} khi người yêu cầu vẫn có quyền và đích còn nhận câu. */
    private String targetProblem(Actor actor, QuizAiDraft draft) {
        if (actor == null) {
            return "AI_DRAFT_FORBIDDEN";
        }
        if (draft.target() == DraftTarget.QUIZ) {
            Optional<Quiz> quiz = quizzes.find(draft.quizId());
            if (quiz.isEmpty()) {
                return "QUIZ_NOT_FOUND";
            }
            if (!support.canManage(actor, quiz.get())) {
                return "AI_DRAFT_FORBIDDEN";
            }
            return quiz.get().isEditable() ? null : "QUIZ_NOT_EDITABLE";
        }
        if (!access.allows(actor, Actions.QUESTION_BANK_MANAGE, ResourceRef.subject(draft.subjectId()))) {
            return "AI_DRAFT_FORBIDDEN";
        }
        try {
            activeSubjects.require(draft.subjectId());
        } catch (ApiException inactive) {
            return inactive.code();
        }
        return null;
    }

    private Optional<QuizAiDraft> draftOf(String targetRef) {
        if (targetRef == null || !targetRef.startsWith(QuizAiDraftService.TARGET_PREFIX)) {
            return Optional.empty();
        }
        try {
            return drafts.find(UUID.fromString(targetRef.substring(QuizAiDraftService.TARGET_PREFIX.length())));
        } catch (IllegalArgumentException malformed) {
            return Optional.empty();
        }
    }

    private void fail(QuizAiDraft draft, String code, String message) {
        drafts.transition(draft.failed(code, message, clock.instant()), DraftStatus.PENDING);
    }
}
