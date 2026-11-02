package edu.aiplatform.grading.service;

import edu.aiplatform.grading.dto.GradingViews;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.assignments.contract.*;
import edu.aiplatform.grading.model.*;
import edu.aiplatform.grading.repository.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.shared.error.ApiException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class GradingMcpQueryServiceTest {
    final ObjectMapper json=new ObjectMapper();
    final SubmissionReviewService reviews=mock(SubmissionReviewService.class);
    final GradingPromptBuilder prompts=mock(GradingPromptBuilder.class);
    final GradingProposalService proposals=mock(GradingProposalService.class);
    final GradebookService gradebook=mock(GradebookService.class);
    final IAssignmentQuery assignments=mock(IAssignmentQuery.class);
    final GradingMcpReadProgress progress=mock(GradingMcpReadProgress.class);
    final GradingMcpQueryService service=new GradingMcpQueryService(reviews,prompts,proposals,gradebook,json,assignments,progress);
    final Actor teacher=new Actor(UUID.randomUUID(),Role.TEACHER);
    Evaluation evaluation() {return Evaluation.pending(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),"hash",BigDecimal.TEN,"[]",Instant.now());}
    void setup(Evaluation e,List<DocumentBlock> blocks) {
        when(reviews.detail(teacher,e.id)).thenReturn(new GradingViews.EvaluationDetail(e,new DocumentModel(blocks),List.of(),List.of(),null,"DISABLED",List.of(),List.of(),List.of(),true));
        when(assignments.getContent(e.assignmentId)).thenReturn(Optional.of(new AssignmentContent(e.assignmentId,1,"Read the case",new DocumentModel(List.of()),List.of(),Map.of(),List.of())));
        when(prompts.gradingInstructions()).thenReturn("Grade only the locked rubric");
        when(prompts.outputSchema(e)).thenReturn("{\"type\":\"object\"}");
    }
    @Test void pagesStructuredBlocksAndRejectsChangedSnapshot() {
        var e=evaluation();List<DocumentBlock> blocks=new ArrayList<>();
        for(int n=0;n<6;n++)blocks.add(new DocumentBlock(UUID.randomUUID(),null,BlockType.PARAGRAPH,json.createObjectNode().put("text","x".repeat(30000))));
        setup(e,blocks);
        var first=service.context(teacher,Map.of("evaluationId",e.id.toString()));
        assertThat(first.get("complete")).isEqualTo(false);
        var continuation=(Map<String,Object>)first.get("continuation");
        var second=service.context(teacher,continuation);
        assertThat(second.get("complete")).isEqualTo(true);
        assertThat((List<?>)first.get("documentBlocks")).isNotEmpty();
        assertThat((List<?>)second.get("documentBlocks")).isNotEmpty();
        e.rubricSnapshot="[{\"changed\":true}]";
        assertThatThrownBy(() -> service.context(teacher,continuation)).isInstanceOf(ApiException.class);
    }
    @Test void groupRootUsesGroupReferenceAndMemberIsRejected() {
        var e=evaluation();e.kind=EvaluationKind.GROUP_DOCUMENT;e.groupDocumentId=UUID.randomUUID();setup(e,List.of());
        assertThat((Map<String,Object>)service.context(teacher,Map.of("evaluationId",e.id.toString())).get("submissionRef"))
                .containsValue("GROUP_DOCUMENT").containsValue(e.groupDocumentId.toString());
        e.kind=EvaluationKind.MEMBER;
        assertThatThrownBy(() -> service.context(teacher,Map.of("evaluationId",e.id.toString()))).isInstanceOf(ApiException.class);
    }
    @Test void oversizedSingleBlockIsExplicitErrorInsteadOfSuccessfulTruncation() {
        var e=evaluation();setup(e,List.of(new DocumentBlock(UUID.randomUUID(),null,BlockType.PARAGRAPH,json.createObjectNode().put("text","x".repeat(250000)))));
        assertThatThrownBy(() -> service.context(teacher,Map.of("evaluationId",e.id.toString())))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("MCP_CONTEXT_TOO_LARGE"));
    }
    @Test void studentCannotReadAndOffsetRequiresSnapshot() {
        assertThatThrownBy(() -> service.context(new Actor(UUID.randomUUID(),Role.STUDENT),Map.of())).isInstanceOf(ApiException.class);
        verifyNoInteractions(reviews);
        var e=evaluation();setup(e,List.of());
        assertThatThrownBy(() -> service.context(teacher,Map.of("evaluationId",e.id.toString(),"offset",1))).isInstanceOf(ApiException.class);
    }
    @Test void gradingContextContainsAssignmentAndSharedPolicy() {
        var e=evaluation();setup(e,List.of());
        var context=service.context(teacher,Map.of("evaluationId",e.id.toString()));
        assertThat(context).containsKeys("assignmentContext", "gradingInstructions");
        assertThat((Map<String,Object>)context.get("assignmentContext")).containsEntry("instructions","Read the case");
        assertThat(context.get("gradingInstructions")).isEqualTo("Grade only the locked rubric");
        verify(progress).advance(teacher.accountId(),e.id,(String)context.get("snapshotFingerprint"),0,0);
        service.requireCompleteRead(teacher,e.id,e.version,e.submissionHash,(String)context.get("snapshotFingerprint"));
        verify(progress).requireComplete(teacher.accountId(),e.id,(String)context.get("snapshotFingerprint"),0);
    }
    @Test void unavailableReadProgressCannotBecomeAContextSuccess() {
        var e=evaluation();setup(e,List.of());
        doThrow(ApiException.unavailable("MCP_CONTEXT_READ_UNAVAILABLE","Unavailable")).when(progress).advance(any(),any(),any(),anyInt(),anyInt());
        assertThatThrownBy(() -> service.context(teacher,Map.of("evaluationId",e.id.toString())))
                .isInstanceOfSatisfying(ApiException.class,ex -> assertThat(ex.code()).isEqualTo("MCP_CONTEXT_READ_UNAVAILABLE"));
    }
    @Test void completedReadOfOldAssignmentCannotAuthorizeANewWrite() {
        var e=evaluation();setup(e,List.of());
        var context=service.context(teacher,Map.of("evaluationId",e.id.toString()));
        when(assignments.getContent(e.assignmentId)).thenReturn(Optional.of(new AssignmentContent(e.assignmentId,2,
                "Changed instructions",new DocumentModel(List.of()),List.of(),Map.of(),List.of())));
        assertThatThrownBy(() -> service.requireCompleteRead(teacher,e.id,e.version,e.submissionHash,(String)context.get("snapshotFingerprint")))
                .isInstanceOfSatisfying(ApiException.class,ex -> assertThat(ex.code()).isEqualTo("SUBMISSION_CHANGED"));
        verify(progress,never()).requireComplete(any(),any(),any(),anyInt());
    }
    @Test void changedAssignmentInstructionsInvalidateContinuationEvenWithTheSameSubmissionHash() {
        var e=evaluation();
        var blocks=List.of(new DocumentBlock(UUID.randomUUID(),null,BlockType.PARAGRAPH,json.createObjectNode().put("text","x".repeat(80000))),
                new DocumentBlock(UUID.randomUUID(),null,BlockType.PARAGRAPH,json.createObjectNode().put("text","x".repeat(80000))));
        setup(e,blocks);
        var first=service.context(teacher,Map.of("evaluationId",e.id.toString()));
        when(assignments.getContent(e.assignmentId)).thenReturn(Optional.of(new AssignmentContent(e.assignmentId,1,
                "Changed instructions",new DocumentModel(List.of()),List.of(),Map.of(),List.of())));
        assertThatThrownBy(() -> service.context(teacher,(Map<String,Object>)first.get("continuation")))
                .isInstanceOfSatisfying(ApiException.class,ex -> assertThat(ex.code()).isEqualTo("SUBMISSION_CHANGED"));
    }
    @Test void discoveryDoesNotLoadSubmissionDocuments() {
        var e=evaluation();
        var rows=new ArrayList<GradingViews.SubmissionRow>();
        var evals=new ArrayList<Evaluation>();
        for(int n=0;n<200;n++) {
            var item=evaluation();item.classId=e.classId;item.assignmentId=e.assignmentId;evals.add(item);
            rows.add(new GradingViews.SubmissionRow(null,item.id,item.attemptId,"PENDING",null,BigDecimal.TEN,false,Instant.now(),1,0));
        }
        when(reviews.listRows(teacher,e.classId,e.assignmentId,"ALL",null)).thenReturn(
                new SubmissionReviewService.Listing(rows,evals,Map.of("total",200L),null));
        var output=(Map<String,Object>)service.call(teacher,"list_submissions",Map.of(
                "classId",e.classId.toString(),"assignmentId",e.assignmentId.toString(),"size",20)).content();
        assertThat((List<?>)output.get("items")).hasSize(20);
        assertThat(output.get("total")).isEqualTo(200);
        verify(reviews,never()).detail(any(),any());
        verify(reviews).listRows(teacher,e.classId,e.assignmentId,"ALL",null);
    }
    @Test void assignmentSummaryAggregatesExistingFeedbackWithExplicitCoverage() {
        var e=evaluation();e.structuredFeedback=new GradingFeedback(1,new GradingFeedback.Summary(List.of("Clear design"),List.of("Explain constraints"),List.of()),List.of());
        setup(e,List.of());
        var row=new GradingViews.SubmissionRow(null,e.id,e.attemptId,"DRAFT",null,BigDecimal.TEN,false,Instant.now(),1,0);
        when(reviews.list(teacher,e.classId,e.assignmentId,"ALL",null,1,100)).thenReturn(
                new GradingViews.SubmissionPage(List.of(row),1,1,100,Map.of("total",1L),null));
        when(reviews.list(teacher,e.classId,e.assignmentId,"ALL",null,1,1)).thenReturn(
                new GradingViews.SubmissionPage(List.of(row),1,1,1,Map.of("total",1L),null));
        var result=(Map<String,Object>)service.call(teacher,"read_class_learning_summary",Map.of("classId",e.classId.toString(),"assignmentId",e.assignmentId.toString())).content();
        assertThat(result.get("feedbackAggregation")).isNotNull();
        assertThat(result.get("feedbackCovered")).isEqualTo(1);
    }
}
