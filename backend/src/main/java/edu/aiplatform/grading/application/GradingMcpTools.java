package edu.aiplatform.grading.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.ai.contract.IMcpToolProvider;
import edu.aiplatform.ai.contract.McpToolDefinition;
import edu.aiplatform.ai.contract.McpToolKind;
import edu.aiplatform.ai.contract.McpToolResult;
import edu.aiplatform.grading.domain.Evaluation;
import edu.aiplatform.grading.domain.EvaluationKind;
import edu.aiplatform.grading.domain.IEvaluationRepository;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.shared.error.ApiException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class GradingMcpTools implements IMcpToolProvider {
    private static final Set<String> ARGUMENT_FIELDS = Set.of("submissionRef", "submissionHash", "proposal",
            "requestRef", "expectedVersion", "agentLabel");
    private static final Set<String> REFERENCE_FIELDS = Set.of("kind", "id", "classId", "assignmentId");
    private final IEvaluationRepository evaluations;
    private final GradingProposalTransactions tx;
    private final ObjectMapper json;

    public GradingMcpTools(IEvaluationRepository evaluations, GradingProposalTransactions tx, ObjectMapper json) {
        this.evaluations = evaluations;
        this.tx = tx;
        this.json = json;
    }

    public List<McpToolDefinition> tools() {
        return List.of(new McpToolDefinition("record_grading_proposal",
                "Record a rubric proposal for teacher review; cannot save or publish official grades.",
                "{\"type\":\"object\",\"additionalProperties\":false,\"required\":[\"submissionRef\",\"submissio"
                        + "nHash\",\"proposal\",\"requestRef\",\"expectedVersion\"],\"properties\":{\"submissionRef\":{\"t"
                        + "ype\":\"object\",\"required\":[\"kind\",\"id\",\"classId\",\"assignmentId\"],\"properties\":{\""
                        + "kind\":{\"enum\":[\"ATTEMPT\",\"GROUP_DOCUMENT\"]},\"id\":{\"type\":\"string\"},\"classId\":{\""
                        + "type\":\"string\"},\"assignmentId\":{\"type\":\"string\"}}},\"submissionHash\":{\"type\":\"stri"
                        + "ng\"},\"proposal\":{\"type\":\"object\"},\"requestRef\":{\"type\":\"string\",\"maxLength\":200}"
                        + ",\"expectedVersion\":{\"type\":\"integer\",\"minimum\":0},\"agentLabel\":{\"type\":\"string\","
                        + "\"maxLength\":200}}}",
                McpToolKind.WRITE, Set.of(Role.TEACHER, Role.SUBJECT_MANAGER)));
    }

    public McpToolResult call(Actor actor, String name, Map<String, Object> args) {
        edu.aiplatform.ai.contract.McpInput.teaching(actor);
        if (!"record_grading_proposal".equals(name))
            throw ApiException.notFound("MCP_TOOL_NOT_FOUND");
        try {
            var root = json.valueToTree(args);
            var reference = readSubmissionReference(root);
            var kind = reference.path("kind").asText();
            var submissionId = UUID.fromString(reference.path("id").asText());
            var classId = UUID.fromString(reference.path("classId").asText());
            var assignmentId = UUID.fromString(reference.path("assignmentId").asText());
            var evaluation = findEvaluation(kind, submissionId, classId, assignmentId);
            if (!classId.equals(evaluation.classId) || !assignmentId.equals(evaluation.assignmentId)
                    || !root.has("expectedVersion")
                    || !root.path("expectedVersion").isIntegralNumber() || root.path("expectedVersion").asLong() < 0)
                throw ApiException.badRequest("INVALID_MCP_ARGUMENTS", "Invalid submission reference or version");
            String requestRef = root.path("requestRef").asText();
            if (requestRef.isBlank() || requestRef.length() > 200)
                throw ApiException.badRequest("INVALID_MCP_ARGUMENTS", "Invalid request reference");
            var proposal = tx.recordMcp(actor, evaluation.id, root.path("expectedVersion").asLong(),
                    root.path("submissionHash").asText(), requestRef, root.get("proposal"),
                    root.path("agentLabel").asText("Agent"));
            return new McpToolResult(false, Map.of("proposalId", proposal.id, "status", proposal.status));
        } catch (ApiException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw ApiException.badRequest("INVALID_MCP_ARGUMENTS", "Invalid grading proposal arguments");
        }
    }

    private JsonNode readSubmissionReference(JsonNode arguments) {
        arguments.fieldNames().forEachRemaining(field -> {
            if (!ARGUMENT_FIELDS.contains(field))
                throw ApiException.badRequest("INVALID_MCP_ARGUMENTS", "Unknown tool argument");
        });
        var reference = arguments.path("submissionRef");
        if (!reference.isObject())
            throw ApiException.badRequest("INVALID_MCP_ARGUMENTS", "Invalid submission reference");
        reference.fieldNames().forEachRemaining(field -> {
            if (!REFERENCE_FIELDS.contains(field))
                throw ApiException.badRequest("INVALID_MCP_ARGUMENTS", "Unknown submission reference argument");
        });
        if (arguments.has("agentLabel")
                && (!arguments.get("agentLabel").isTextual() || arguments.get("agentLabel").asText().length() > 200))
            throw ApiException.badRequest("INVALID_MCP_ARGUMENTS", "Invalid agent label");
        return reference;
    }

    private Evaluation findEvaluation(String kind, UUID submissionId, UUID classId, UUID assignmentId) {
        if (kind.equals("ATTEMPT"))
            return evaluations.findByAttemptId(submissionId)
                    .orElseThrow(() -> ApiException.notFound("GRADE_NOT_FOUND"));
        if (kind.equals("GROUP_DOCUMENT"))
            return evaluations.findForAssignmentAndClass(assignmentId, classId).stream()
                    .filter(evaluation -> evaluation.kind == EvaluationKind.GROUP_DOCUMENT
                            && submissionId.equals(evaluation.groupDocumentId))
                    .findFirst().orElseThrow(() -> ApiException.notFound("GRADE_NOT_FOUND"));
        throw ApiException.badRequest("INVALID_MCP_ARGUMENTS", "Invalid submission kind");
    }
}
