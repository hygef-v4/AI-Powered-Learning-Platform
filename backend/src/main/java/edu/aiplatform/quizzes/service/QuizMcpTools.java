package edu.aiplatform.quizzes.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.ai.contract.IMcpToolProvider;
import edu.aiplatform.ai.contract.McpToolDefinition;
import edu.aiplatform.ai.contract.McpToolKind;
import edu.aiplatform.ai.contract.McpToolResult;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.quizzes.service.AiDraftViews.AiDraftView;
import edu.aiplatform.shared.error.ApiException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Tool MCP của U10 (F13, H7): chỉ giảng viên, Chủ nhiệm môn có quyền với Quiz; người học bị từ chối (BR-U10-42).
 * Tool ghi chỉ lưu đề xuất chờ người soạn chấp nhận; U06 kiểm credential {@code READ_WRITE} cho tool {@code WRITE}.
 */
@Component
public class QuizMcpTools implements IMcpToolProvider {

    static final String READ_CONTEXT = "read_quiz_context";
    static final String SAVE_DRAFT = "save_quiz_draft";
    private static final Set<Role> ROLES = Set.of(Role.TEACHER, Role.SUBJECT_MANAGER);
    private static final int MAX_AGENT_LABEL = 200;

    private final QuizAiDraftService drafts;
    private final ObjectMapper json;

    public QuizMcpTools(QuizAiDraftService drafts, ObjectMapper json) {
        this.drafts = drafts;
        this.json = json;
    }

    @Override
    public List<McpToolDefinition> tools() {
        return List.of(
                new McpToolDefinition(READ_CONTEXT,
                        "Đọc quiz bạn có quyền sửa: tiêu đề, cài đặt, câu, lựa chọn, đáp án, giải thích, version.",
                        """
                        {"type":"object","additionalProperties":false,"required":["quizId"],\
                        "properties":{"quizId":{"type":"string","format":"uuid"}}}""",
                        McpToolKind.READ, ROLES),
                new McpToolDefinition(SAVE_DRAFT,
                        "Lưu câu đề xuất cho quiz đang nháp; người soạn xem và chấp nhận trên Quiz Detail. "
                                + "Không sửa, duyệt hay phát hành quiz.",
                        """
                        {"type":"object","additionalProperties":false,\
                        "required":["quizId","expectedVersion","questions"],"properties":{\
                        "quizId":{"type":"string","format":"uuid"},\
                        "expectedVersion":{"type":"integer","minimum":0},\
                        "questions":{"type":"array","minItems":1,"maxItems":50,"items":{"type":"object",\
                        "additionalProperties":false,"required":["type","stem","options","correct"],"properties":{\
                        "type":{"enum":["MCQ_SINGLE","MCQ_MULTI"]},"stem":{"type":"string"},\
                        "options":{"type":"array","items":{"type":"string"}},\
                        "correct":{"type":"array","items":{"type":"integer","minimum":1}},\
                        "explanation":{"type":"string"},"difficulty":{"enum":["EASY","MEDIUM","HARD"]},\
                        "points":{"type":"number","exclusiveMinimum":0}}}},\
                        "agentLabel":{"type":"string","maxLength":200}}}""",
                        McpToolKind.WRITE, ROLES));
    }

    @Override
    public McpToolResult call(Actor actor, String toolName, Map<String, Object> args) {
        if (!READ_CONTEXT.equals(toolName) && !SAVE_DRAFT.equals(toolName)) {
            throw ApiException.notFound("MCP_TOOL_NOT_FOUND");
        }
        if (actor == null || !ROLES.contains(actor.role())) {
            throw ApiException.forbidden("MCP_TOOL_FORBIDDEN");
        }
        JsonNode root = json.valueToTree(args == null ? Map.of() : args);
        if (READ_CONTEXT.equals(toolName)) {
            fields(root, Set.of("quizId"));
            return new McpToolResult(false, drafts.mcpContext(actor, quizId(root)));
        }
        fields(root, Set.of("quizId", "expectedVersion", "questions", "agentLabel"));
        JsonNode version = root.get("expectedVersion");
        if (version == null || !version.canConvertToLong() || version.asLong() < 0) {
            throw invalid("expectedVersion cần là số nguyên ≥ 0");
        }
        JsonNode label = root.get("agentLabel");
        if (label != null && !label.isNull() && (!label.isTextual() || label.asText().length() > MAX_AGENT_LABEL)) {
            throw invalid("agentLabel tối đa 200 ký tự");
        }
        AiDraftView draft = drafts.saveFromMcp(actor, quizId(root), version.asLong(), root.get("questions"),
                label == null || label.isNull() ? null : label.asText());
        long invalidCount = draft.questions().stream().filter(q -> !q.violations().isEmpty()).count();
        return new McpToolResult(false, Map.of("draftId", draft.id(), "status", draft.status().name(),
                "questions", draft.questions().size(), "invalidQuestions", invalidCount));
    }

    private static void fields(JsonNode root, Set<String> allowed) {
        if (!root.isObject()) {
            throw invalid("Tham số cần là object");
        }
        root.fieldNames().forEachRemaining(name -> {
            if (!allowed.contains(name)) {
                throw invalid("Tham số không được phép: " + name);
            }
        });
    }

    private static UUID quizId(JsonNode root) {
        JsonNode value = root.get("quizId");
        if (value == null || !value.isTextual()) {
            throw invalid("Thiếu quizId");
        }
        try {
            return UUID.fromString(value.asText());
        } catch (IllegalArgumentException malformed) {
            throw invalid("quizId không hợp lệ");
        }
    }

    private static ApiException invalid(String message) {
        return ApiException.badRequest("INVALID_MCP_ARGUMENTS", message);
    }
}
