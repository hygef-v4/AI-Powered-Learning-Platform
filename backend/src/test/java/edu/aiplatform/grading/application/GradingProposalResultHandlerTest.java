package edu.aiplatform.grading.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.ai.contract.*;
import edu.aiplatform.assignments.contract.*;
import edu.aiplatform.grading.domain.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.shared.error.ApiException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class GradingProposalResultHandlerTest {
    @Test
    void failedTaskPreservesSafeDiagnosticWithoutChangingGrade() {
        var repo = mock(IEvaluationRepository.class);
        var proposals = mock(IGradingProposalRepository.class);
        var e = new Evaluation(); e.id = UUID.randomUUID();
        var p = new GradingProposal(); p.id = UUID.randomUUID(); p.evaluationId = e.id;
        p.taskId = UUID.randomUUID(); p.requesterId = UUID.randomUUID(); p.status = "PENDING";
        when(repo.lockById(e.id)).thenReturn(Optional.of(e));
        when(proposals.findForEvaluation(e.id)).thenReturn(List.of(p));
        when(proposals.findEvaluationId(p.id)).thenReturn(Optional.of(e.id));
        when(proposals.findById(p.id)).thenReturn(Optional.of(p));
        when(proposals.save(any())).thenAnswer(i -> i.getArgument(0));
        var json = new ObjectMapper().findAndRegisterModules();
        var tx = new GradingProposalTransactions(repo, proposals, mock(GradingService.class), mock(GradingAccess.class),
                mock(SubmissionReviewService.class), mock(IAssignmentQuery.class), mock(GradingPromptBuilder.class),
                new GradingProposalValidator(json), json, Clock.systemUTC());
        String diagnostic = "Nhà cung cấp AI từ chối yêu cầu (HTTP 400): model hoặc tham số không hợp lệ";
        tx.failed(new AiTaskFailure(p.taskId, "GRADING_PROPOSAL", new Actor(p.requesterId, Role.TEACHER),
                "evaluation:" + e.id, AiTaskState.FAILED, diagnostic));
        assertThat(p.safeError).isEqualTo(diagnostic);
        assertThat(p.status).isEqualTo("FAILED");
        assertThat(e.score).isNull();
        // Polling can recover the diagnostic for failures saved by an older U09 version.
        p.safeError = "AI task failed";
        tx.dispatched(p.id, new AiTaskView(p.taskId, AiTaskState.FAILED, AiSource.PERSONAL, AiProvider.GEMINI,
                "model", null, diagnostic, null), null);
        assertThat(p.safeError).isEqualTo(diagnostic);
    }

    @Test
    void failedCallbackBeforeTaskIdAttachmentMatchesByInputVersionAndPreservesFailureAcrossDispatch() {
        var repo = mock(IEvaluationRepository.class);
        var proposals = mock(IGradingProposalRepository.class);
        var e = new Evaluation();
        e.id = UUID.randomUUID();
        var p = new GradingProposal();
        p.id = UUID.randomUUID();
        p.evaluationId = e.id;
        p.versionNo = 2;
        p.taskId = null;
        p.requesterId = UUID.randomUUID();
        p.status = "PENDING";
        when(repo.lockById(e.id)).thenReturn(Optional.of(e));
        when(proposals.findForEvaluation(e.id)).thenReturn(List.of(p));
        when(proposals.findEvaluationId(p.id)).thenReturn(Optional.of(e.id));
        when(proposals.findById(p.id)).thenReturn(Optional.of(p));
        when(proposals.save(any())).thenAnswer(i -> i.getArgument(0));
        var json = new ObjectMapper().findAndRegisterModules();
        var tx = new GradingProposalTransactions(repo, proposals, mock(GradingService.class), mock(GradingAccess.class),
                mock(SubmissionReviewService.class), mock(IAssignmentQuery.class), mock(GradingPromptBuilder.class),
                new GradingProposalValidator(json), json, Clock.systemUTC());
        UUID taskId = UUID.randomUUID();
        String diagnostic = "Nhà cung cấp AI quá hạn phản hồi";
        tx.failed(new AiTaskFailure(taskId, "GRADING_PROPOSAL", new Actor(p.requesterId, Role.TEACHER),
                "evaluation:" + e.id, 2L, AiTaskState.UNKNOWN, diagnostic));
        assertThat(p.status).isEqualTo("FAILED");
        assertThat(p.taskId).isEqualTo(taskId);
        assertThat(p.safeError).isEqualTo(diagnostic);
        assertThat(p.completedAt).isNotNull();

        // Subsequent dispatched() call with initial QUEUED view must not revert FAILED status.
        var queuedView = new AiTaskView(taskId, AiTaskState.QUEUED, AiSource.SHARED, AiProvider.GEMINI,
                "gemini-2.5-flash", null, null, null);
        var afterDispatch = tx.dispatched(p.id, queuedView, null);
        assertThat(afterDispatch.status).isEqualTo("FAILED");
        assertThat(afterDispatch.safeError).isEqualTo(diagnostic);
        assertThat(afterDispatch.taskState).isNull();
    }

    @Test
    void readyCallbackBeforeTaskIdAttachmentTransitionsImmediatelyAndIdempotently() throws Exception {
        var repo = mock(IEvaluationRepository.class);
        var proposals = mock(IGradingProposalRepository.class);
        var grading = mock(GradingService.class);
        var access = mock(GradingAccess.class);
        var json = new ObjectMapper().findAndRegisterModules();
        UUID rubricId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        var rubrics = List.of(new RubricView(
                rubricId, UUID.randomUUID(), "part-1",
                List.of(new RubricCriterion(UUID.randomUUID(), "Kiến trúc", "", CriterionKind.values()[0],
                        List.of(new RubricItem(itemId, "Đúng quan hệ", new BigDecimal("2.50"))))),
                new BigDecimal("2.50")));
        var e = Evaluation.pending(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "hash-1", new BigDecimal("2.50"), json.writeValueAsString(rubrics), Instant.now());
        var p = new GradingProposal();
        p.id = UUID.randomUUID();
        p.evaluationId = e.id;
        p.versionNo = 1;
        p.taskId = null;
        p.submissionHash = "hash-1";
        p.rubricFingerprint = e.rubricSnapshot;
        p.requesterId = UUID.randomUUID();
        when(repo.lockById(e.id)).thenReturn(Optional.of(e));
        when(proposals.findForEvaluation(e.id)).thenReturn(List.of(p));
        when(proposals.findEvaluationId(p.id)).thenReturn(Optional.of(e.id));
        when(proposals.findById(p.id)).thenReturn(Optional.of(p));
        when(proposals.save(any())).thenAnswer(i -> i.getArgument(0));
        var handler = new GradingProposalResultHandler(new GradingProposalTransactions(
                repo, proposals, grading, access, mock(SubmissionReviewService.class),
                mock(IAssignmentQuery.class), mock(GradingPromptBuilder.class),
                new GradingProposalValidator(json), json, Clock.systemUTC()));
        UUID taskId = UUID.randomUUID();
        String output = """
                {"checks":{"%s":["%s"]},"itemScoresByRubric":{"%s":{"%s":2.00}},"score":2.00,"feedback":"Tốt","sections":[]}
                """.formatted(rubricId, itemId, rubricId, itemId);
        var result = new AiTaskResult(taskId, "GRADING_PROPOSAL", new Actor(p.requesterId, Role.TEACHER),
                "evaluation:" + e.id, 1, output);

        assertThat(handler.onReady(result)).isEqualTo(ApplyOutcome.APPLIED);
        assertThat(p.status).isEqualTo("READY");
        assertThat(p.taskId).isEqualTo(taskId);
        assertThat(p.completedAt).isNotNull();
        assertThat(e.score).isNull();

        // Duplicate delivery is idempotent and does not re-save or alter proposal.
        assertThat(handler.onReady(result)).isEqualTo(ApplyOutcome.APPLIED);
        verify(proposals, times(1)).save(p);
    }

    @Test
    void transient5xxDependencyErrorRethrowsForRetryWhile4xxMarksRejectedWithoutChangingGrade() throws Exception {
        var repo = mock(IEvaluationRepository.class);
        var proposals = mock(IGradingProposalRepository.class);
        var grading = mock(GradingService.class);
        var access = mock(GradingAccess.class);
        var json = new ObjectMapper().findAndRegisterModules();
        var e = Evaluation.pending(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "hash", BigDecimal.TEN, "[]", Instant.now());
        var p = new GradingProposal();
        p.id = UUID.randomUUID();
        p.evaluationId = e.id;
        p.versionNo = 1;
        p.submissionHash = "hash";
        p.rubricFingerprint = "[]";
        p.requesterId = UUID.randomUUID();
        when(repo.lockById(e.id)).thenReturn(Optional.of(e));
        when(proposals.findForEvaluation(e.id)).thenReturn(List.of(p));
        when(proposals.save(any())).thenAnswer(i -> i.getArgument(0));
        doThrow(ApiException.unavailable("SUBMISSION_UNAVAILABLE", "Submission service temporarily unavailable"))
                .doThrow(ApiException.forbidden("OUT_OF_CLASS_SCOPE"))
                .when(access).requireTeacher(any(), eq(e.classId), eq(Actions.GRADING_WRITE));
        var tx = new GradingProposalTransactions(repo, proposals, grading, access,
                mock(SubmissionReviewService.class), mock(IAssignmentQuery.class), mock(GradingPromptBuilder.class),
                new GradingProposalValidator(json), json, Clock.systemUTC());
        var result = new AiTaskResult(UUID.randomUUID(), "GRADING_PROPOSAL", new Actor(p.requesterId, Role.TEACHER),
                "evaluation:" + e.id, 1, "{\"checks\":{},\"score\":0,\"feedback\":\"ok\",\"sections\":[]}");

        // First attempt: 503 rethrows so worker retries handler without losing proposal.
        assertThatThrownBy(() -> tx.accept(result)).isInstanceOf(ApiException.class);
        assertThat(p.status).isEqualTo("PENDING");
        verify(proposals, never()).save(any());

        // Second attempt: 403 marks proposal REJECTED and leaves official grade untouched.
        assertThat(tx.accept(result)).isEqualTo(ApplyOutcome.REJECTED);
        assertThat(p.status).isEqualTo("REJECTED");
        assertThat(p.taskId).isEqualTo(result.taskId());
        assertThat(p.completedAt).isNotNull();
        assertThat(e.score).isNull();
    }

    @Test
    void invalidAiOutputMarksProposalRejectedWithoutMutatingEvaluation() {
        var repo = mock(IEvaluationRepository.class);
        var proposals = mock(IGradingProposalRepository.class);
        var grading = mock(GradingService.class);
        var access = mock(GradingAccess.class);
        var json = new ObjectMapper().findAndRegisterModules();
        var e = Evaluation.pending(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "hash", BigDecimal.TEN, "[]", Instant.now());
        var p = new GradingProposal();
        p.id = UUID.randomUUID();
        p.evaluationId = e.id;
        p.versionNo = 1;
        p.submissionHash = "hash";
        p.rubricFingerprint = "[]";
        p.requesterId = UUID.randomUUID();
        when(repo.lockById(e.id)).thenReturn(Optional.of(e));
        when(proposals.findForEvaluation(e.id)).thenReturn(List.of(p));
        when(proposals.save(any())).thenAnswer(i -> i.getArgument(0));
        var tx = new GradingProposalTransactions(repo, proposals, grading, access,
                mock(SubmissionReviewService.class), mock(IAssignmentQuery.class), mock(GradingPromptBuilder.class),
                new GradingProposalValidator(json), json, Clock.systemUTC());
        var result = new AiTaskResult(UUID.randomUUID(), "GRADING_PROPOSAL", new Actor(p.requesterId, Role.TEACHER),
                "evaluation:" + e.id, 1, "{not-valid-json");

        assertThat(tx.accept(result)).isEqualTo(ApplyOutcome.REJECTED);
        assertThat(p.status).isEqualTo("REJECTED");
        assertThat(p.completedAt).isNotNull();
        assertThat(e.score).isNull();
    }

    @Test
    void staleCallbackDoesNotChangeOfficialGrade() {
        var repo = mock(IEvaluationRepository.class);
        var proposals = mock(IGradingProposalRepository.class);
        var grading = mock(GradingService.class);
        var access = mock(GradingAccess.class);
        var review = mock(SubmissionReviewService.class);
        var assignments = mock(IAssignmentQuery.class);
        var json = new ObjectMapper().findAndRegisterModules();
        var e = Evaluation.pending(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "new-hash", BigDecimal.TEN, "[]", Instant.now());
        var p = new GradingProposal();
        p.id = UUID.randomUUID();
        p.evaluationId = e.id;
        p.versionNo = 1;
        p.submissionHash = "old-hash";
        p.rubricFingerprint = "[]";
        p.requesterId = UUID.randomUUID();
        when(repo.lockById(e.id)).thenReturn(Optional.of(e));
        when(proposals.findForEvaluation(e.id)).thenReturn(List.of(p));
        when(proposals.save(any())).thenAnswer(i -> i.getArgument(0));
        var tx = new GradingProposalTransactions(repo, proposals, grading, access, review, assignments,
                new GradingPromptBuilder(mock(IDocumentModel.class), json), new GradingProposalValidator(json), json,
                Clock.systemUTC());
        var result = new AiTaskResult(UUID.randomUUID(), "GRADING_PROPOSAL", new Actor(p.requesterId, Role.TEACHER),
                "evaluation:" + e.id, 1, "{}");
        assertThat(tx.accept(result)).isEqualTo(ApplyOutcome.STALE);
        assertThat(e.score).isNull();
        assertThat(p.status).isEqualTo("STALE");
        assertThat(p.taskId).isEqualTo(result.taskId());
        assertThat(p.completedAt).isNotNull();
    }

    @Test
    void dispatchAttachesToAlreadyReadyCallbackWithoutReadingBeforeLock() {
        var repo = mock(IEvaluationRepository.class);
        var proposals = mock(IGradingProposalRepository.class);
        var p = new GradingProposal();
        p.id = UUID.randomUUID();
        p.evaluationId = UUID.randomUUID();
        p.status = "READY";
        when(proposals.findEvaluationId(p.id)).thenReturn(Optional.of(p.evaluationId));
        when(repo.lockById(p.evaluationId)).thenReturn(Optional.of(new Evaluation()));
        when(proposals.findById(p.id)).thenReturn(Optional.of(p));
        when(proposals.save(any())).thenAnswer(i -> i.getArgument(0));
        var json = new ObjectMapper().findAndRegisterModules();
        var tx = new GradingProposalTransactions(repo, proposals, mock(GradingService.class), mock(GradingAccess.class),
                mock(SubmissionReviewService.class), mock(IAssignmentQuery.class), mock(GradingPromptBuilder.class),
                new GradingProposalValidator(json), json, Clock.systemUTC());
        var task = new AiTaskView(UUID.randomUUID(), AiTaskState.QUEUED, AiSource.SHARED, null, null, null, null, null);
        assertThat(tx.dispatched(p.id, task, null).status).isEqualTo("READY");
        assertThat(p.taskState).isNull();
        var order = inOrder(proposals, repo);
        order.verify(proposals).findEvaluationId(p.id);
        order.verify(repo).lockById(p.evaluationId);
        order.verify(proposals).findById(p.id);
    }

    @Test
    void readyProposalBecomesStaleAfterGroupSnapshotChanges() {
        var repo = mock(IEvaluationRepository.class);
        var proposals = mock(IGradingProposalRepository.class);
        var e = new Evaluation();
        e.id = UUID.randomUUID();
        e.submissionHash = "new";
        e.rubricSnapshot = "[]";
        var p = new GradingProposal();
        p.id = UUID.randomUUID();
        p.evaluationId = e.id;
        p.status = "READY";
        p.submissionHash = "old";
        p.rubricFingerprint = "[]";
        when(repo.lockById(e.id)).thenReturn(Optional.of(e));
        when(proposals.findForEvaluation(e.id)).thenReturn(List.of(p));
        var json = new ObjectMapper().findAndRegisterModules();
        var tx = new GradingProposalTransactions(repo, proposals, mock(GradingService.class), mock(GradingAccess.class),
                mock(SubmissionReviewService.class), mock(IAssignmentQuery.class), mock(GradingPromptBuilder.class),
                new GradingProposalValidator(json), json, Clock.systemUTC());
        tx.invalidateChangedSnapshot(e.id);
        assertThat(p.status).isEqualTo("STALE");
        assertThat(e.score).isNull();
        verify(proposals).save(p);
    }
}
