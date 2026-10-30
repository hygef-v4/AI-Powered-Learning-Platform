package edu.aiplatform.assignmentwork.application;

import static edu.aiplatform.assignmentwork.application.WorkFixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import edu.aiplatform.ai.contract.McpToolResult;
import edu.aiplatform.ai.contract.IMcpToolProvider;
import edu.aiplatform.ai.application.*;
import edu.aiplatform.ai.domain.McpCredential;
import org.springframework.beans.factory.ObjectProvider;
import edu.aiplatform.assignments.contract.*;
import edu.aiplatform.assignmentwork.contract.AttemptStatus;
import edu.aiplatform.assignmentwork.domain.PracticeFeedback;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.Role;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Real eligibility/normalization service behind the restored MCP transport, not a mocked tool result. */
@SuppressWarnings("unchecked")
class PracticeFeedbackMcpParityTest {
    final WorkFixtures f = new WorkFixtures();
    final AssignmentWorkMcpTools tools = new AssignmentWorkMcpTools(f.feedbackService, f.access, f.documents, f.json);
    final Map<String,Object> output = Map.of("summary","Góp ý", "strengths", java.util.List.of("Rõ"),
            "parts",java.util.List.of(Map.of("partId","DOCUMENT","comment","Thêm ví dụ")),
            "suggestions",java.util.List.of("Bổ sung"),"estimatedTotal",10);

    @BeforeEach void practice() {
        f.view=f.view(AssignmentStatus.OPEN,SubmissionMode.INDIVIDUAL,GradingMode.PRACTICE,
                NOW.plus(Duration.ofDays(1)),null,3);
    }
    Map<String,Object> args(UUID id,String hash) {
        return Map.of("attemptId",id.toString(),"submissionHash",hash,"feedback",output,"agentLabel","Own agent");
    }
    void error(McpToolResult result,String code) {
        assertThat(result.error()).isTrue();
        assertThat((Map<String,Object>)result.content()).containsEntry("code",code);
        verify(f.feedback,never()).insert(any());
    }
    @Test void ownSubmittedPracticeIsReadableAndAgentFeedbackUsesSamePrivateStore() {
        var attempt=f.attempt(1,AttemptStatus.SUBMITTED,NOW,false);
        var read=tools.call(f.student,"read_own_work",Map.of("attemptId",attempt.id().toString()));
        assertThat(read.error()).isFalse();
        assertThat((Map<String,Object>)read.content()).containsKeys("instructions","assignment","submission","submissionHash")
                .doesNotContainKeys("rubric","rubrics","correctAnswers");
        assertThat((Map<String,Object>)read.content()).containsEntry("submissionHash",attempt.receiptHash());
        var saved=tools.call(f.student,"record_private_feedback",args(attempt.id(),attempt.receiptHash()));
        assertThat(saved.error()).isFalse();
        var captured=ArgumentCaptor.forClass(PracticeFeedback.class);
        verify(f.feedback).insert(captured.capture());
        assertThat(captured.getValue().origin()).isEqualTo(PracticeFeedback.Origin.MCP);
        assertThat(captured.getValue().accountId()).isEqualTo(f.student.accountId());
        assertThat(captured.getValue().submissionHash()).isEqualTo(attempt.receiptHash());
        assertThat(captured.getValue().feedback().toString()).doesNotContain("estimatedTotal","rubric");
        verifyNoInteractions(f.aiTasks);
    }
    @Test void staleHashCannotPersistFeedback() {
        var a=f.attempt(1,AttemptStatus.SUBMITTED,NOW,false);
        error(tools.call(f.student,"record_private_feedback",args(a.id(),"stale")),"SUBMISSION_HASH_MISMATCH");
    }
    @Test void pendingProviderRequestAlsoBlocksIncomingAgentFeedback() {
        var a=f.attempt(1,AttemptStatus.SUBMITTED,NOW,false);
        when(f.feedback.hasPending(a.id())).thenReturn(true);
        error(tools.call(f.student,"record_private_feedback",args(a.id(),a.receiptHash())),"FEEDBACK_PENDING");
    }
    @Test void dailyQuotaCountsBothTransports() {
        var a=f.attempt(1,AttemptStatus.SUBMITTED,NOW,false);
        when(f.feedback.countSince(eq(f.student.accountId()),any())).thenReturn(20L);
        error(tools.call(f.student,"record_private_feedback",args(a.id(),a.receiptHash())),"FEEDBACK_LIMIT");
    }
    @Test void workingAndGradedAttemptsAreNotEligible() {
        var working=f.attempt(1,AttemptStatus.IN_PROGRESS,NOW,false);
        error(tools.call(f.student,"read_own_work",Map.of("attemptId",working.id().toString())),"FEEDBACK_NOT_ALLOWED");
        f.view=f.view(AssignmentStatus.OPEN,SubmissionMode.INDIVIDUAL,GradingMode.GRADED,NOW.plusSeconds(60),null,3);
        var graded=f.attempt(2,AttemptStatus.SUBMITTED,NOW,false);
        error(tools.call(f.student,"record_private_feedback",args(graded.id(),graded.receiptHash())),"FEEDBACK_NOT_ALLOWED");
    }
    @Test void foreignOwnerAndRemovedEnrollmentCannotReadOrWrite() {
        var a=f.attempt(1,AttemptStatus.SUBMITTED,NOW,false);
        var other=new Actor(UUID.randomUUID(),Role.STUDENT);
        assertThat(tools.call(other,"read_own_work",Map.of("attemptId",a.id().toString())).error()).isTrue();
        when(f.authorization.authorize(eq(f.student),anyString(),any()))
                .thenReturn(edu.aiplatform.identity.contract.AuthorizationDecision.deny("ENROLLMENT_REMOVED"));
        assertThat(tools.call(f.student,"record_private_feedback",args(a.id(),a.receiptHash())).error()).isTrue();
        verify(f.feedback,never()).insert(any());
    }

    @Test void protocolDispatchesRealLearnerFeedbackAndReadOnlyCredentialCannotWrite() throws Exception {
        ObjectProvider<IMcpToolProvider> providers=mock(ObjectProvider.class);
        when(providers.orderedStream()).thenAnswer(call->java.util.stream.Stream.of(tools,new McpCapabilitiesTools()));
        IMcpRateLimiter limiter=mock(IMcpRateLimiter.class);
        when(limiter.tryAcquire(any())).thenReturn(true);
        var server=new McpServer(new McpToolRegistry(providers),limiter,new JsonSchemaChecker(f.json),
                mock(AiUsageService.class),f.audit,f.json,f.clock);
        var a=f.attempt(1,AttemptStatus.SUBMITTED,NOW,false);
        var write=new McpCredential(UUID.randomUUID(),f.student.accountId(),"Own practice agent",
                McpCredential.Access.READ_WRITE,"hash","hint",NOW.plusSeconds(60),null,null,NOW);
        var principal=new McpCredentialService.McpPrincipal(write,f.student);
        var list=server.handle(principal,f.json.valueToTree(Map.of("jsonrpc","2.0","id",1,"method","tools/list"))).orElseThrow();
        assertThat(list.at("/result/tools").toString()).contains("read_own_work","record_private_feedback")
                .doesNotContain("read_grading_context","read_quiz_context");
        Map<String,Object> valid=Map.of("summary","Góp ý","parts",java.util.List.of(Map.of("partId","DOCUMENT","comment","Ổn")),
                "suggestions",java.util.List.of("Bổ sung"));
        var request=f.json.valueToTree(Map.of("jsonrpc","2.0","id",2,"method","tools/call","params",
                Map.of("name","record_private_feedback","arguments",Map.of("attemptId",a.id().toString(),
                        "submissionHash",a.receiptHash(),"feedback",valid))));
        var readOnly=new McpCredential(write.id(),write.accountId(),write.label(),McpCredential.Access.READ_ONLY,
                write.tokenHash(),write.tokenHint(),write.expiresAt(),null,null,NOW);
        var denied=server.handle(new McpCredentialService.McpPrincipal(readOnly,f.student),request).orElseThrow();
        assertThat(denied.has("error") || denied.at("/result/isError").asBoolean()).isTrue();
        verify(f.feedback,never()).insert(any());
        var response=server.handle(principal,request).orElseThrow();
        assertThat(response.at("/result/isError").isBoolean()).isTrue();
        assertThat(response.at("/result/isError").asBoolean()).isFalse();
        assertThat(response.at("/result/content/0/text").asText()).contains("feedbackId","READY");
        verify(f.feedback).insert(any());
        verifyNoInteractions(f.aiTasks);
    }
}
