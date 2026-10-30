package edu.aiplatform.grading.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import edu.aiplatform.ai.contract.*;
import edu.aiplatform.grading.service.GradingCommands.*;
import edu.aiplatform.grading.model.*;
import edu.aiplatform.grading.repository.*;
import edu.aiplatform.identity.contract.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GradingProposalServiceTest {
    @Test
    void longestBatchReferenceDerivesBoundedStablePerItemReference() {
        var tx = mock(GradingProposalTransactions.class);
        var ai = mock(IAiTask.class);
        var evaluations = mock(IEvaluationRepository.class);
        var proposals = mock(IGradingProposalRepository.class);
        var access = mock(GradingAccess.class);
        var e = new Evaluation();
        e.id = UUID.randomUUID();
        e.classId = UUID.randomUUID();
        when(evaluations.findById(e.id)).thenReturn(Optional.of(e));
        var p = new GradingProposal();
        p.status = "READY";
        when(tx.prepare(any(), any(), any())).thenReturn(new GradingProposalTransactions.Prepared(p, null, false, null));
        var actor = new Actor(UUID.randomUUID(), Role.TEACHER);
        var input = new AiBatch(List.of(new ItemVersion(e.id, 0)),
                new AiRequest(AiSource.SHARED, null, "r".repeat(200)));
        var service = new GradingProposalService(tx, ai, evaluations, proposals, access);
        service.requestBatch(actor, input);
        service.requestBatch(actor, input);
        var captor = ArgumentCaptor.forClass(AiRequest.class);
        verify(tx, times(2)).prepare(eq(actor), eq(e.id), captor.capture());
        assertThat(captor.getAllValues().get(0).requestRef()).hasSizeLessThanOrEqualTo(200)
                .isEqualTo(captor.getAllValues().get(1).requestRef());
        verifyNoInteractions(ai);
    }

    @Test
    void sourceFailureStopsFurtherBatchDispatchWithoutFallback() {
        var tx = mock(GradingProposalTransactions.class);
        var ai = mock(IAiTask.class);
        var evaluations = mock(IEvaluationRepository.class);
        var p = new GradingProposal();
        p.status = "FAILED";
        p.safeError = "QUOTA_EXHAUSTED";
        when(tx.prepare(any(), any(), any())).thenReturn(new GradingProposalTransactions.Prepared(p, null, false, null));
        var e = new Evaluation();
        e.id = UUID.randomUUID();
        e.classId = UUID.randomUUID();
        when(evaluations.findById(e.id)).thenReturn(Optional.of(e));
        var other = UUID.randomUUID();
        var service = new GradingProposalService(tx, ai, evaluations, mock(IGradingProposalRepository.class),
                mock(GradingAccess.class));
        var result = service.requestBatch(new Actor(UUID.randomUUID(), Role.TEACHER),
                new AiBatch(List.of(new ItemVersion(e.id, 0), new ItemVersion(other, 0)),
                        new AiRequest(AiSource.PERSONAL, AiProvider.OPENAI, "batch")));
        assertThat(result.items()).extracting(v -> v.outcome()).containsExactly("FAILED", "SKIPPED");
        verify(evaluations, never()).findById(other);
        verifyNoInteractions(ai);
    }

    @Test
    void batchEstimateSumsEachDistinctCurrentEvaluationWithoutDispatchingAi() {
        var tx = mock(GradingProposalTransactions.class);
        var ai = mock(IAiTask.class);
        var first = UUID.randomUUID();
        var second = UUID.randomUUID();
        var actor = new Actor(UUID.randomUUID(), Role.TEACHER);
        when(tx.estimateTokens(actor, first, 2)).thenReturn(5_100L);
        when(tx.estimateTokens(actor, second, 4)).thenReturn(6_200L);
        var service = new GradingProposalService(tx, ai, mock(IEvaluationRepository.class),
                mock(IGradingProposalRepository.class), mock(GradingAccess.class));

        var estimate = service.estimateBatch(actor,
                new Batch(List.of(new ItemVersion(first, 2), new ItemVersion(second, 4), new ItemVersion(first, 2))));

        assertThat(estimate.estimatedTokens()).isEqualTo(11_300L);
        assertThat(estimate.items()).isEqualTo(2);
        verify(tx).estimateTokens(actor, first, 2);
        verify(tx).estimateTokens(actor, second, 4);
        verifyNoInteractions(ai);
    }

    @Test
    void listPopulatesLiveTaskStateWithoutWritingWhilePendingAndReconcilesRejectedOrFailedTasks() {
        var tx = mock(GradingProposalTransactions.class);
        var ai = mock(IAiTask.class);
        var evaluations = mock(IEvaluationRepository.class);
        var proposals = mock(IGradingProposalRepository.class);
        var access = mock(GradingAccess.class);
        var e = new Evaluation();
        e.id = UUID.randomUUID();
        e.classId = UUID.randomUUID();
        when(evaluations.findById(e.id)).thenReturn(Optional.of(e));

        var pendingRunning = new GradingProposal();
        pendingRunning.id = UUID.randomUUID();
        pendingRunning.evaluationId = e.id;
        pendingRunning.status = "PENDING";
        pendingRunning.taskId = UUID.randomUUID();

        var pendingReadyUnapplied = new GradingProposal();
        pendingReadyUnapplied.id = UUID.randomUUID();
        pendingReadyUnapplied.evaluationId = e.id;
        pendingReadyUnapplied.status = "PENDING";
        pendingReadyUnapplied.taskId = UUID.randomUUID();

        when(proposals.findForEvaluation(e.id)).thenReturn(List.of(pendingRunning, pendingReadyUnapplied));
        when(ai.get(pendingRunning.taskId)).thenReturn(Optional.of(new AiTaskView(
                pendingRunning.taskId, AiTaskState.RUNNING, AiSource.PERSONAL, AiProvider.GEMINI,
                "gemini-2.5-flash", null, null, null)));
        when(ai.get(pendingReadyUnapplied.taskId)).thenReturn(Optional.of(new AiTaskView(
                pendingReadyUnapplied.taskId, AiTaskState.READY, AiSource.PERSONAL, AiProvider.GEMINI,
                "gemini-2.5-flash", null, null, null)));

        var service = new GradingProposalService(tx, ai, evaluations, proposals, access);
        var actor = new Actor(UUID.randomUUID(), Role.TEACHER);
        var listed = service.list(actor, e.id);

        assertThat(listed).extracting(p -> p.taskState).containsExactly("RUNNING", "READY");
        verify(tx, never()).dispatched(any(), any(), any());

        // When a READY task was rejected at delivery, list() reconciles it via dispatched().
        var rejectedTask = new AiTaskView(
                pendingReadyUnapplied.taskId, AiTaskState.READY, AiSource.PERSONAL, AiProvider.GEMINI,
                "gemini-2.5-flash", ApplyOutcome.REJECTED, "Tài khoản yêu cầu không còn hiệu lực", null);
        when(ai.get(pendingReadyUnapplied.taskId)).thenReturn(Optional.of(rejectedTask));
        service.list(actor, e.id);
        verify(tx).dispatched(pendingReadyUnapplied.id, rejectedTask, null);
    }
}
