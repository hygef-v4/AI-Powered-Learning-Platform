package edu.aiplatform.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.ai.contract.*;
import edu.aiplatform.ai.model.McpCredential.Access;
import edu.aiplatform.identity.contract.Actor;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class McpAuthoringRequirementsTools implements IMcpToolProvider {
    private static final String ASSIGNMENT_WORKFLOW = "Read read_agent_workflow with "
            + "workflow=AUTHOR_ASSIGNMENT for complete server-managed instructions. Continue "
            + "clarification in external agent chat, use these current schemas, create only "
            + "DRAFT/proposals and return reviewUrl for human review. No local skill installation "
            + "required.";
    private static final String QUIZ_WORKFLOW = "Resolve the authorized target and ask one "
            + "grouped clarification in external agent chat for missing topic/materials, "
            + "difficulty and points; never guess IDs or silently change the scale. Use these "
            + "current schemas and save only a draft/proposal for human review. The assignment "
            + "workflow does not replace QUIZ/BANK schemas.";

    private final McpToolRegistry registry;
    private final ObjectMapper json;

    // Nhận registry để đọc schema hiện hành thay vì duy trì một bản sao.
    public McpAuthoringRequirementsTools(McpToolRegistry registry, ObjectMapper json) {
        this.registry = registry;
        this.json = json;
    }

    // Khai báo các loại nội dung có thể tra yêu cầu soạn thảo.
    @Override
    public List<McpToolDefinition> tools() {
        return List.of(McpSchemas.read("read_authoring_requirements",
                "Đọc schema proposal và tạo nháp từ chính registry hiện hành trước khi soạn nội dung.",
                "\"kind\":{\"enum\":[\"ASSIGNMENT\",\"QUIZ\",\"BANK\"]}", "kind"));
    }

    // Tra schema lưu và tạo mới theo loại nội dung từ registry cùng giới hạn rubric cho agent.
    @Override
    public McpToolResult call(Actor actor, String name, Map<String, Object> args) {
        McpInput.requireTeaching(actor);
        if (!"read_authoring_requirements".equals(name)) {
            throw McpInput.createInvalidArguments();
        }
        String kind = new McpInput(args).readText("kind", "");
        String proposal;
        String create;
        switch (kind) {
            case "ASSIGNMENT" -> {
                proposal = "save_assignment_draft";
                create = "create_assignment_draft";
            }
            case "QUIZ" -> {
                proposal = "save_quiz_draft";
                create = "create_quiz_draft";
            }
            case "BANK" -> {
                proposal = "save_question_bank_draft";
                create = proposal;
            }
            default -> throw McpInput.createInvalidArguments();
        }
        try {
            var save = registry.findTool(proposal, actor.role(), Access.READ_WRITE)
                    .orElseThrow(McpInput::createInvalidArguments);
            var creation = registry.findTool(create, actor.role(), Access.READ_WRITE)
                    .orElseThrow(McpInput::createInvalidArguments);
            return new McpToolResult(false, Map.of(
                    "kind", kind,
                    "proposalInputSchema", json.readTree(save.definition().parameterSchema()),
                    "createInputSchema", json.readTree(creation.definition().parameterSchema()),
                    "maxAiGradingRubricItems", AiGradingLimits.MAX_RUBRIC_ITEMS,
                    "workflow", "ASSIGNMENT".equals(kind) ? ASSIGNMENT_WORKFLOW : QUIZ_WORKFLOW));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Invalid registered MCP schema", e);
        }
    }
}
