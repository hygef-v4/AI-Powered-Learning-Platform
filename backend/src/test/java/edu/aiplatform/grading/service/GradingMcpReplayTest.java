package edu.aiplatform.grading.service;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.assignments.contract.AssignmentContent;
import edu.aiplatform.assignments.contract.BlockOrigin;
import edu.aiplatform.assignments.contract.BlockType;
import edu.aiplatform.assignments.contract.CriterionKind;
import edu.aiplatform.assignments.contract.DocumentBlock;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.DocumentPart;
import edu.aiplatform.assignments.contract.IAssignmentQuery;
import edu.aiplatform.assignments.contract.RubricCriterion;
import edu.aiplatform.assignments.contract.RubricItem;
import edu.aiplatform.assignments.contract.RubricView;
import edu.aiplatform.grading.dto.GradingViews;
import edu.aiplatform.grading.model.*;
import edu.aiplatform.grading.repository.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.shared.error.ApiException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
class GradingMcpReplayTest {
    @Test
    void wrongPartEvidenceKeepsProposedPointsAndRequiresReviewOnReplay() throws Exception {
        var json = new ObjectMapper();
        var actor = new Actor(UUID.randomUUID(), Role.TEACHER);
        var assignmentId = UUID.randomUUID();
        var diagram = UUID.randomUUID();
        var spec = UUID.randomUUID();
        var evidence = UUID.randomUUID();
        var rubricId = UUID.randomUUID();
        var itemId = UUID.randomUUID();
        var parts = List.of(new DocumentPart(diagram.toString(), diagram, List.of(), "Diagram"),
                new DocumentPart(spec.toString(), spec, List.of(), "Specification"));
        var document = new DocumentModel(List.of(
                new DocumentBlock(diagram, BlockOrigin.TEACHER, BlockType.HEADING,
                        json.createObjectNode().put("style", "H1")),
                new DocumentBlock(spec, BlockOrigin.TEACHER, BlockType.HEADING,
                        json.createObjectNode().put("style", "H1")),
                new DocumentBlock(evidence, BlockOrigin.STUDENT, BlockType.PARAGRAPH,
                        json.createObjectNode().put("text", "Secondary actor"))));
        var rubrics = List.of(new RubricView(rubricId, assignmentId, diagram.toString(),
                List.of(new RubricCriterion(UUID.randomUUID(), "Notation", "", CriterionKind.DIAGRAM,
                        List.of(new RubricItem(itemId, "Boundary", BigDecimal.ONE)))), BigDecimal.ONE));
        var evaluation = Evaluation.createPendingEvaluation(UUID.randomUUID(), assignmentId,
                UUID.randomUUID(), UUID.randomUUID(), "hash", BigDecimal.ONE,
                json.writeValueAsString(rubrics), Instant.now());
        var gradingService = mock(GradingService.class);
        when(gradingService.requireCurrent(actor, evaluation.id, 0L)).thenReturn(evaluation);
        var proposals = mock(IGradingProposalRepository.class);
        var submissionReviewService = mock(SubmissionReviewService.class);
        var detail = mock(GradingViews.EvaluationDetail.class);
        when(detail.document()).thenReturn(document);
        when(submissionReviewService.loadEvaluation(actor, evaluation.id)).thenReturn(detail);
        var assignments = mock(IAssignmentQuery.class);
        when(assignments.getContent(assignmentId)).thenReturn(Optional.of(
                new AssignmentContent(assignmentId, 1, "", document, parts, Map.of(), rubrics)));
        when(proposals.saveProposal(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var transaction = new GradingProposalTransactions(null, proposals, gradingService, null,
                submissionReviewService, assignments, null, new GradingProposalValidator(json), json, Clock.systemUTC());
        var payload = json.readTree("""
                {"checks":{"%1$s":["%2$s"]},"score":"1.00","itemScoresByRubric":{"%1$s":{"%2$s":1.00}},
                "structuredFeedback":{"schemaVersion":1,"summary":{"strengths":[],"improvements":[],"nextSteps":[]},
                "items":[{"rubricId":"%1$s","itemId":"%2$s","evidence":"Secondary actor","reason":"Observed",
                "evidenceBlockId":"%3$s","requiresReview":false}]}}
                """.formatted(rubricId, itemId, evidence));

        var saved = transaction.recordMcpProposal(actor, evaluation.id, 0, "hash", "ref", payload, "Agent");
        var feedback = saved.proposal.at("/structuredFeedback/items/0");
        assertThat(feedback.path("requiresReview").asBoolean()).isTrue();
        assertThat(feedback.path("reviewReason").asText()).isNotBlank();
        assertThat(saved.proposal.path("score").asText()).isEqualTo("1.00");
        assertThat(payload.at("/structuredFeedback/items/0/requiresReview").asBoolean()).isFalse();

        when(proposals.findByRequestRef("ref")).thenReturn(Optional.of(saved));
        assertThat(transaction.recordMcpProposal(actor, evaluation.id, 0, "hash", "ref", payload, "Agent"))
                .isSameAs(saved);
        var changed = payload.deepCopy();
        ((com.fasterxml.jackson.databind.node.ObjectNode) changed.at("/structuredFeedback/items/0"))
                .put("reason", "Changed reasoning");
        assertThatThrownBy(() -> transaction.recordMcpProposal(actor, evaluation.id, 0, "hash", "ref",
                changed, "Agent"))
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.readCode()).isEqualTo("REQUEST_REF_CONFLICT"));
        var malformed = payload.deepCopy();
        ((com.fasterxml.jackson.databind.node.ObjectNode) malformed.at("/structuredFeedback/items/0"))
                .putObject("reviewReason").put("reason", "not a string");
        assertThatThrownBy(() -> transaction.recordMcpProposal(actor, evaluation.id, 0, "hash", "ref",
                malformed, "Agent"))
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.readCode()).isEqualTo("REQUEST_REF_CONFLICT"));
        verify(proposals, times(1)).saveProposal(any());
    }

    @Test void unavailableSubmissionContextCannotBecomeAReadyProposal() throws Exception {
        var json=new ObjectMapper();var actor=new Actor(UUID.randomUUID(),Role.TEACHER);
        var e=Evaluation.createPendingEvaluation(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),"hash",BigDecimal.TEN,"[]",Instant.now());
        var grading=mock(GradingService.class);when(grading.requireCurrent(actor,e.id,0L)).thenReturn(e);
        var proposals=mock(IGradingProposalRepository.class);var review=mock(SubmissionReviewService.class);
        when(review.loadEvaluation(actor,e.id)).thenThrow(ApiException.createUnavailable("ASSIGNMENT_UNAVAILABLE","Unavailable"));
        var tx=new GradingProposalTransactions(null,proposals,grading,null,review,null,null,new GradingProposalValidator(json),json,Clock.systemUTC());
        var payload=json.readTree("""
                {"checks":{},"score":"0","itemScoresByRubric":{},"structuredFeedback":{
                  "schemaVersion":1,"summary":{"strengths":[],"improvements":[],"nextSteps":[]},"items":[]}}
                """);
        assertThatThrownBy(() -> tx.recordMcpProposal(actor,e.id,0,"hash","new",payload,"Agent"))
                .isInstanceOfSatisfying(ApiException.class,ex -> assertThat(ex.readCode()).isEqualTo("ASSIGNMENT_UNAVAILABLE"));
        verify(proposals,never()).saveProposal(any());
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
        verify(proposals,never()).saveProposal(any());
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
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.readCode()).isEqualTo("STALE_PROPOSAL"));
    }
}
