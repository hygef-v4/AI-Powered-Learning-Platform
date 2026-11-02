package edu.aiplatform.assignmentwork.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.ai.contract.IMcpToolProvider;
import edu.aiplatform.ai.contract.McpToolDefinition;
import edu.aiplatform.ai.contract.McpToolKind;
import edu.aiplatform.ai.contract.McpToolResult;
import edu.aiplatform.assignments.contract.IDocumentModel;
import edu.aiplatform.assignmentwork.domain.AssignmentAttempt;
import edu.aiplatform.assignmentwork.domain.PracticeFeedback;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.shared.error.ApiException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Tool MCP của U08 (F10): người học đọc bài Practice đã nộp của mình và ghi góp ý riêng; giảng viên chính của lớp đọc
 * bài đã nộp của lớp mình. Ngoài quyền trả lỗi, có audit.
 */
@Component
public class AssignmentWorkMcpTools implements IMcpToolProvider {

    static final String READ_OWN = "read_own_work";
    static final String RECORD = "record_private_feedback";
    static final String READ_SUBMISSION = "read_authorized_submission";

    private final PracticeFeedbackService feedback;
    private final WorkAccess access;
    private final IDocumentModel documents;
    private final ObjectMapper json;

    public AssignmentWorkMcpTools(PracticeFeedbackService feedback, WorkAccess access, IDocumentModel documents,
                                  ObjectMapper json) {
        this.feedback = feedback;
        this.access = access;
        this.documents = documents;
        this.json = json;
    }

    @Override
    public List<McpToolDefinition> tools() {
        String attempt = "{\"type\":\"object\",\"required\":[\"attemptId\"],\"properties\":{\"attemptId\":"
                + "{\"type\":\"string\",\"format\":\"uuid\"}}}";
        return List.of(
                new McpToolDefinition(READ_SUBMISSION, "Đọc bài đã nộp của sinh viên lớp bạn dạy.",
                        attempt, McpToolKind.READ, Set.of(Role.TEACHER, Role.SUBJECT_MANAGER)));
    }

    @Override
    public McpToolResult call(Actor actor, String toolName, Map<String, Object> args) {
        try {
            UUID attemptId = UUID.fromString(String.valueOf(args.get("attemptId")));
            return switch (toolName) {
                case READ_SUBMISSION -> new McpToolResult(false, submission(actor, attemptId));
                default -> error("TOOL_NOT_FOUND", "Không có tool này");
            };
        } catch (ApiException e) {
            return error(e.code(), e.getMessage());
        } catch (IllegalArgumentException | NullPointerException e) {
            return error("INVALID_ARGUMENTS", "Tham số không hợp lệ");
        }
    }

    /** F10 bước 4: giảng viên chính của lớp ghi trên lượt, lượt đã nộp; không có dữ liệu người khác. */
    private Map<String, Object> submission(Actor actor, UUID attemptId) {
        if (actor.role() == Role.STUDENT) {
            throw access.deny(actor, READ_SUBMISSION, WorkAudit.OBJECT_ATTEMPT, attemptId, "ROLE_NOT_ALLOWED");
        }
        AssignmentAttempt attempt = access.requireReadableAttempt(actor, attemptId, READ_SUBMISSION);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("attemptId", attempt.id().toString());
        out.put("assignmentId", attempt.assignmentId().toString());
        out.put("title", attempt.snapshot().title());
        out.put("attemptNo", attempt.attemptNo());
        out.put("submittedAt", attempt.submittedAt() == null ? null : attempt.submittedAt().toString());
        out.put("late", attempt.late());
        out.put("submission", documents.toPlainText(attempt.content()));
        return out;
    }

    private static McpToolResult error(String code, String message) {
        return new McpToolResult(true, Map.of("code", code, "message", message));
    }
}
