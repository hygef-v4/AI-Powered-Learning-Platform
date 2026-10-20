package edu.aiplatform.grading.application;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import org.mockito.ArgumentCaptor;
import java.util.*;
import edu.aiplatform.grading.domain.*;
import edu.aiplatform.grading.application.GradingCommands.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.ai.contract.*;

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
        when(tx.prepare(any(), any(), any())).thenReturn(new GradingProposalTransactions.Prepared(p, null, false));
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
        when(tx.prepare(any(), any(), any())).thenReturn(new GradingProposalTransactions.Prepared(p, null, false));
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
}
