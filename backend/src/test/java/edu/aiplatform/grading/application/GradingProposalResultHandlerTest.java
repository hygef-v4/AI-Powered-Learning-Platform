package edu.aiplatform.grading.application;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import java.time.*;
import java.util.*;
import java.math.BigDecimal;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.grading.domain.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.ai.contract.*;
import edu.aiplatform.assignments.contract.*;

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
