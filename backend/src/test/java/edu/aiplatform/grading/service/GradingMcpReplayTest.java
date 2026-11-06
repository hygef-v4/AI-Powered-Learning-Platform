package edu.aiplatform.grading.service;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.grading.model.*;
import edu.aiplatform.grading.repository.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.shared.error.ApiException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
class GradingMcpReplayTest {
    @Test void unavailableSubmissionContextCannotBecomeAReadyProposal() throws Exception {
        var json=new ObjectMapper();var actor=new Actor(UUID.randomUUID(),Role.TEACHER);
        var e=Evaluation.createPendingEvaluation(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),"hash",BigDecimal.TEN,"[]",Instant.now());
        var grading=mock(GradingService.class);when(grading.requireCurrent(actor,e.id,0L)).thenReturn(e);
        var proposals=mock(IGradingProposalRepository.class);var review=mock(SubmissionReviewService.class);
        when(review.loadEvaluation(actor,e.id)).thenThrow(ApiException.unavailable("ASSIGNMENT_UNAVAILABLE","Unavailable"));
        var tx=new GradingProposalTransactions(null,proposals,grading,null,review,null,null,new GradingProposalValidator(json),json,Clock.systemUTC());
        var payload=json.readTree("""
                {"checks":{},"score":"0","itemScoresByRubric":{},"structuredFeedback":{
                  "schemaVersion":1,"summary":{"strengths":[],"improvements":[],"nextSteps":[]},"items":[]}}
                """);
        assertThatThrownBy(() -> tx.recordMcpProposal(actor,e.id,0,"hash","new",payload,"Agent"))
                .isInstanceOfSatisfying(ApiException.class,ex -> assertThat(ex.code()).isEqualTo("ASSIGNMENT_UNAVAILABLE"));
        verify(proposals,never()).save(any());
    }
    @Test void exactLegacyReplayDoesNotRevalidateHistoricalPayload() {
        var json=new ObjectMapper();var actor=new Actor(UUID.randomUUID(),Role.TEACHER);
        var e=Evaluation.createPendingEvaluation(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),"hash",BigDecimal.TEN,"[]",Instant.now());
        var grading=mock(GradingService.class);when(grading.requireCurrent(actor,e.id,0L)).thenReturn(e);
        var proposals=mock(IGradingProposalRepository.class);var validator=mock(GradingProposalValidator.class);
        var existing=new GradingProposal();existing.evaluationId=e.id;existing.requesterId=actor.accountId();
        existing.submissionHash="hash";existing.rubricFingerprint=e.rubricSnapshot;existing.proposal=json.createObjectNode();
        when(proposals.findByRequestRef("retry")).thenReturn(Optional.of(existing));
        var tx=new GradingProposalTransactions(null,proposals,grading,null,null,null,null,validator,json,Clock.systemUTC());
        assertThat(tx.recordMcpProposal(actor,e.id,0,"hash","retry",existing.proposal,"Agent")).isSameAs(existing);
        verifyNoInteractions(validator);
        verify(proposals,never()).save(any());
    }
    @Test void retryCannotReturnAProposalFromAnOlderRubricSnapshot() {
        var json=new ObjectMapper();var actor=new Actor(UUID.randomUUID(),Role.TEACHER);
        var evaluation=Evaluation.createPendingEvaluation(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),"hash",BigDecimal.TEN,"[]",Instant.now());
        var grading=mock(GradingService.class);when(grading.requireCurrent(actor,evaluation.id,0L)).thenReturn(evaluation);
        var proposals=mock(IGradingProposalRepository.class);var existing=new GradingProposal();
        existing.id=UUID.randomUUID();existing.evaluationId=evaluation.id;existing.requesterId=actor.accountId();
        existing.submissionHash="hash";existing.rubricFingerprint="previous-rubric";existing.proposal=json.createObjectNode();
        when(proposals.findByRequestRef("retry")).thenReturn(Optional.of(existing));
        var tx=new GradingProposalTransactions(null,proposals,grading,null,null,null,null,mock(GradingProposalValidator.class),json,Clock.systemUTC());
        assertThatThrownBy(() -> tx.recordMcpProposal(actor,evaluation.id,0,"hash","retry",json.createObjectNode(),"Agent"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("STALE_PROPOSAL"));
    }
}
