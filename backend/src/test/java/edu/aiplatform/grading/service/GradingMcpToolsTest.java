package edu.aiplatform.grading.service;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.grading.model.*;
import edu.aiplatform.grading.repository.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.ai.contract.*;
import edu.aiplatform.shared.error.ApiException;

class GradingMcpToolsTest {
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = EvaluationKind.class, names = {"ATTEMPT", "GROUP_DOCUMENT"})
    void recordsProposalForEitherSubmissionKindWithLegacyDefaults(EvaluationKind kind) {
        var repo = mock(IEvaluationRepository.class);
        var tx = mock(GradingProposalTransactions.class);
        var actor = new Actor(UUID.randomUUID(), Role.TEACHER);
        var evaluation = new Evaluation();
        evaluation.id = UUID.randomUUID();
        evaluation.kind = kind;
        evaluation.classId = UUID.randomUUID();
        evaluation.assignmentId = UUID.randomUUID();
        var submissionId = UUID.randomUUID();
        evaluation.groupDocumentId = submissionId;
        when(repo.findByAttemptId(submissionId)).thenReturn(Optional.of(evaluation));
        when(repo.findForAssignmentAndClass(evaluation.assignmentId, evaluation.classId))
                .thenReturn(List.of(evaluation));
        var proposal = new GradingProposal();
        proposal.id = UUID.randomUUID();
        proposal.status = "READY";
        var json = new ObjectMapper();
        var payload = Map.of("checks", Map.of(), "score", "0");
        when(tx.recordMcp(eq(actor), eq(evaluation.id), eq(0L), eq("hash"), eq("request"),
                eq(json.valueToTree(payload)), eq("Agent"))).thenReturn(proposal);
        var arguments = Map.<String, Object>of("submissionRef", Map.of("kind", kind.name(),
                "id", submissionId.toString(), "classId", evaluation.classId.toString(),
                "assignmentId", evaluation.assignmentId.toString()), "expectedVersion", 0,
                "submissionHash", "hash", "requestRef", "request", "proposal", payload);

        var result = new GradingMcpTools(repo, tx, json).call(actor, "record_grading_proposal", arguments);

        assertThat(result).isEqualTo(new McpToolResult(false,
                Map.of("proposalId", proposal.id, "status", "READY")));
        verify(tx).recordMcp(actor, evaluation.id, 0L, "hash", "request", json.valueToTree(payload), "Agent");
    }

    @Test
    void rejectsUnknownReferenceFieldsBeforeParsingIdsOrReadingRepository() {
        var repo = mock(IEvaluationRepository.class);
        var tx = mock(GradingProposalTransactions.class);
        var tools = new GradingMcpTools(repo, tx, new ObjectMapper());
        assertThatThrownBy(() -> tools.call(new Actor(UUID.randomUUID(), Role.TEACHER),
                "record_grading_proposal", Map.of("submissionRef", Map.of("unexpected", "value"))))
                .isInstanceOfSatisfying(ApiException.class, error -> {
                    assertThat(error.code()).isEqualTo("INVALID_MCP_ARGUMENTS");
                    assertThat(error.getMessage()).isEqualTo("Unknown submission reference argument");
                });
        verifyNoInteractions(repo, tx);
    }

    @Test
    void missingEvaluationPrecedesInvalidVersionAndRequestReference() {
        var repo = mock(IEvaluationRepository.class);
        var tx = mock(GradingProposalTransactions.class);
        var submissionId = UUID.randomUUID();
        when(repo.findByAttemptId(submissionId)).thenReturn(Optional.empty());
        var arguments = Map.<String, Object>of("submissionRef", Map.of("kind", "ATTEMPT",
                "id", submissionId.toString(), "classId", UUID.randomUUID().toString(),
                "assignmentId", UUID.randomUUID().toString()), "expectedVersion", -1, "requestRef", "");

        assertThatThrownBy(() -> new GradingMcpTools(repo, tx, new ObjectMapper()).call(
                new Actor(UUID.randomUUID(), Role.TEACHER), "record_grading_proposal", arguments))
                .isInstanceOfSatisfying(ApiException.class, error -> {
                    assertThat(error.status().value()).isEqualTo(404);
                    assertThat(error.code()).isEqualTo("GRADE_NOT_FOUND");
                });
        verifyNoInteractions(tx);
    }

    @Test
    void malformedSubmissionIdUsesTheExistingGenericArgumentError() {
        var repo = mock(IEvaluationRepository.class);
        var tx = mock(GradingProposalTransactions.class);
        assertThatThrownBy(() -> new GradingMcpTools(repo, tx, new ObjectMapper()).call(
                new Actor(UUID.randomUUID(), Role.TEACHER), "record_grading_proposal",
                Map.of("submissionRef", Map.of("kind", "ATTEMPT", "id", "malformed"))))
                .isInstanceOfSatisfying(ApiException.class, error -> {
                    assertThat(error.status().value()).isEqualTo(400);
                    assertThat(error.code()).isEqualTo("INVALID_MCP_ARGUMENTS");
                    assertThat(error.getMessage()).isEqualTo("Invalid grading proposal arguments");
                });
        verifyNoInteractions(repo, tx);
    }

    @Test
    void exposesOnlyProposalWriteToolForTeachingRoles() {
        var tools = new GradingMcpTools(mock(IEvaluationRepository.class), mock(GradingProposalTransactions.class),
                new ObjectMapper());
        assertThat(tools.tools()).hasSize(1);
        var tool = tools.tools().get(0);
        assertThat(tool.name()).isEqualTo("record_grading_proposal");
        assertThat(tool.kind()).isEqualTo(McpToolKind.WRITE);
        assertThat(tool.allowedRoles()).containsExactlyInAnyOrder(Role.TEACHER, Role.SUBJECT_MANAGER);
    }

    @Test
    void spoofedActorArgumentIsRejectedBeforeWritingAnything() {
        var repo = mock(IEvaluationRepository.class);
        var tx = mock(GradingProposalTransactions.class);
        var tools = new GradingMcpTools(repo, tx, new ObjectMapper());
        assertThatThrownBy(() -> tools.call(new Actor(UUID.randomUUID(), Role.STUDENT), "record_grading_proposal",
                Map.of("actor", Map.of("role", "TEACHER")))).isInstanceOf(ApiException.class);
        verifyNoInteractions(repo, tx);
    }

    @Test
    void scopeAndStaleVersionErrorsFromTransactionalGuardArePreserved() {
        var repo = mock(IEvaluationRepository.class);
        var tx = mock(GradingProposalTransactions.class);
        var e = new Evaluation();
        e.id = UUID.randomUUID();
        e.attemptId = UUID.randomUUID();
        e.classId = UUID.randomUUID();
        e.assignmentId = UUID.randomUUID();
        when(repo.findByAttemptId(e.attemptId)).thenReturn(Optional.of(e));
        when(tx.recordMcp(any(), eq(e.id), eq(2L), any(), any(), any(), any()))
                .thenThrow(ApiException.conflict("GRADE_VERSION_CONFLICT", "Reload"));
        var args = Map.<String, Object>of("submissionRef",
                Map.of("kind", "ATTEMPT", "id", e.attemptId.toString(), "classId", e.classId.toString(), "assignmentId",
                        e.assignmentId.toString()),
                "expectedVersion", 2, "submissionHash", "hash", "requestRef", "ref", "proposal",
                Map.of("checks", Map.of(), "score", "0"));
        var tools = new GradingMcpTools(repo, tx, new ObjectMapper());
        assertThatThrownBy(
                () -> tools.call(new Actor(UUID.randomUUID(), Role.TEACHER), "record_grading_proposal", args))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.status().value()).isEqualTo(409));
    }
}
