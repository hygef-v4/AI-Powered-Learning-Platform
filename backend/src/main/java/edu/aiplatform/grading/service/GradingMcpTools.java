package edu.aiplatform.grading.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.ai.contract.IMcpToolProvider;
import edu.aiplatform.ai.contract.McpToolDefinition;
import edu.aiplatform.ai.contract.McpToolKind;
import edu.aiplatform.ai.contract.McpToolResult;
import edu.aiplatform.grading.model.Evaluation;
import edu.aiplatform.grading.model.EvaluationKind;
import edu.aiplatform.grading.repository.IEvaluationRepository;
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
    private static final Set<String> ARGUMENT_FIELDS = Set.of("submissionRef", "submissionHash",
            "proposal", "requestRef", "expectedVersion", "agentLabel", "snapshotFingerprint");
    private static final Set<String> REFERENCE_FIELDS = Set.of("kind", "id", "classId",
            "assignmentId");
    private final IEvaluationRepository evaluationRepository;
    private final GradingProposalTransactions gradingProposalTransactions;
    private final ObjectMapper objectMapper;
    private final GradingMcpQueryService gradingMcpQueryService;

    /** Creates the grading proposal MCP provider with evaluation and proposal services. */
    public GradingMcpTools(IEvaluationRepository evaluationRepository,
            GradingProposalTransactions gradingProposalTransactions, ObjectMapper objectMapper,
            GradingMcpQueryService gradingMcpQueryService) {
        this.evaluationRepository = evaluationRepository;
        this.gradingProposalTransactions = gradingProposalTransactions;
        this.objectMapper = objectMapper;
        this.gradingMcpQueryService = gradingMcpQueryService;
    }

    /** Defines the MCP tool that records a grading proposal for teacher review. */
    public List<McpToolDefinition> tools() {
        return List.of(new McpToolDefinition("record_grading_proposal",
                "Record a rubric proposal after reading every current read_grading_context page. Pass its snapshotFingerprint; return reviewUrl to the user. Cannot save or publish official grades.",
                "{\"type\":\"object\",\"additionalProperties\":false,\"required\":[\"submissionRef\",\"submissio"
                        + "nHash\",\"proposal\",\"requestRef\",\"expectedVersion\",\"snapshotFingerprint\"],\"properties\":{\"submissionRef\":{\"t"
                        + "ype\":\"object\",\"required\":[\"kind\",\"id\",\"classId\",\"assignmentId\"],\"properties\":{\""
                        + "kind\":{\"enum\":[\"ATTEMPT\",\"GROUP_DOCUMENT\"]},\"id\":{\"type\":\"string\"},\"classId\":{\""
                        + "type\":\"string\"},\"assignmentId\":{\"type\":\"string\"}}},\"submissionHash\":{\"type\":\"stri"
                        + "ng\"},\"proposal\":{\"type\":\"object\"},\"requestRef\":{\"type\":\"string\",\"maxLength\":200}"
                        + ",\"expectedVersion\":{\"type\":\"integer\",\"minimum\":0},\"agentLabel\":{\"type\":\"string\","
                        + "\"maxLength\":200},\"snapshotFingerprint\":{\"type\":\"string\",\"pattern\":\"^[0-9a-f]{64}$\"}}}",
                McpToolKind.WRITE, Set.of(Role.TEACHER, Role.SUBJECT_MANAGER)));
    }

    /** Validates the submission reference and records its grading proposal. */
    public McpToolResult call(Actor actor, String name, Map<String, Object> args) {
        // Step 1: Verify caller holds active teacher or subject manager permission
        edu.aiplatform.ai.contract.McpInput.teaching(actor);
        if (!"record_grading_proposal".equals(name))
            throw ApiException.notFound("MCP_TOOL_NOT_FOUND");
        try {
            // Step 2: Parse and validate submission reference (kind, id, classId, assignmentId)
            var root = objectMapper.valueToTree(args);
            var reference = readSubmissionReference(root);
            var kind = reference.path("kind").asText();
            var submissionId = UUID.fromString(reference.path("id").asText());
            var classId = UUID.fromString(reference.path("classId").asText());
            var assignmentId = UUID.fromString(reference.path("assignmentId").asText());
            var evaluation = findEvaluation(kind, submissionId, classId, assignmentId);
            if (!classId.equals(evaluation.classId) || !assignmentId.equals(evaluation.assignmentId)
                    || !root.has("expectedVersion")
                    || !root.path("expectedVersion").isIntegralNumber()
                    || root.path("expectedVersion").asLong() < 0)
                throw ApiException.badRequest("INVALID_MCP_ARGUMENTS",
                        "Invalid submission reference or version");

            // Step 3: Check request reference and snapshot fingerprint format (64-char hex)
            String requestRef = root.path("requestRef").asText();
            if (requestRef.isBlank() || requestRef.length() > 200)
                throw ApiException.badRequest("INVALID_MCP_ARGUMENTS", "Invalid request reference");
            String fingerprint = root.path("snapshotFingerprint").asText();
            if (!fingerprint.matches("[0-9a-f]{64}"))
                throw GradingMcpReadProgress.createReadRequiredError();

            // Step 4: Enforce complete read requirement - verify agent read 100% of submission blocks
            gradingMcpQueryService.requireCompleteRead(actor, evaluation.id,
                    root.path("expectedVersion").asLong(), root.path("submissionHash").asText(),
                    fingerprint);

            // Step 5: Store proposal idempotently in database via transaction service
            var proposal = gradingProposalTransactions.recordMcpProposal(actor, evaluation.id,
                    root.path("expectedVersion").asLong(), root.path("submissionHash").asText(),
                    requestRef, root.get("proposal"), root.path("agentLabel").asText("Agent"));

            // Step 6: Construct teacher review link and return proposal reference to caller
            return new McpToolResult(false,
                    Map.of("proposalId", proposal.id, "status", proposal.status, "evaluationId",
                            evaluation.id, "reviewUrl",
                            buildReviewUrl(evaluation.id, proposal.id)));
        } catch (ApiException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw ApiException.badRequest("INVALID_MCP_ARGUMENTS",
                    "Invalid grading proposal arguments");
        }
    }

    /** Builds the teacher review link for a proposal. */
    static String buildReviewUrl(UUID evaluationId, UUID proposalId) {
        return "/teaching/evaluations/" + evaluationId + "?proposalId=" + proposalId
                + "#grading-ai-review";
    }

    /** Validates allowed tool arguments and returns the submission reference object. */
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
                throw ApiException.badRequest("INVALID_MCP_ARGUMENTS",
                        "Unknown submission reference argument");
        });
        if (arguments.has("agentLabel") && (!arguments.get("agentLabel").isTextual()
                || arguments.get("agentLabel").asText().length() > 200))
            throw ApiException.badRequest("INVALID_MCP_ARGUMENTS", "Invalid agent label");
        return reference;
    }

    /** Finds the evaluation identified by an attempt or group document reference. */
    private Evaluation findEvaluation(String kind, UUID submissionId, UUID classId,
            UUID assignmentId) {
        if (kind.equals("ATTEMPT"))
            return evaluationRepository.findByAttemptId(submissionId)
                    .orElseThrow(() -> ApiException.notFound("GRADE_NOT_FOUND"));
        if (kind.equals("GROUP_DOCUMENT"))
            return evaluationRepository.findForAssignmentAndClass(assignmentId, classId).stream()
                    .filter(evaluation -> evaluation.kind == EvaluationKind.GROUP_DOCUMENT
                            && submissionId.equals(evaluation.groupDocumentId))
                    .findFirst().orElseThrow(() -> ApiException.notFound("GRADE_NOT_FOUND"));
        throw ApiException.badRequest("INVALID_MCP_ARGUMENTS", "Invalid submission kind");
    }
}
