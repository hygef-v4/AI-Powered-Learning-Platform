package edu.aiplatform.assignments.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.academics.contract.IClassAccess;
import edu.aiplatform.ai.contract.IMcpToolProvider;
import edu.aiplatform.ai.contract.McpToolDefinition;
import edu.aiplatform.ai.contract.McpToolKind;
import edu.aiplatform.ai.contract.McpToolResult;
import edu.aiplatform.assignments.contract.AssignmentScope;
import edu.aiplatform.assignments.contract.AssignmentStatus;
import edu.aiplatform.assignments.contract.RubricView;
import edu.aiplatform.assignments.domain.Assignment;
import edu.aiplatform.assignments.domain.AssignmentAiDraft;
import edu.aiplatform.assignments.domain.AssignmentRepository;
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
 * Tool MCP của U07 (F15; BR-U07-53): {@code read_assignment_context} cho giảng viên có quyền và người học của lớp;
 * {@code save_assignment_draft} chỉ lưu đề xuất chờ người soạn áp dụng. Ngoài quyền trả lỗi, có audit.
 */
@Component
public class AssignmentMcpTools implements IMcpToolProvider {

    static final String READ = "read_assignment_context";
    static final String SAVE = "save_assignment_draft";

    private final AssignmentRepository assignments;
    private final AssignmentAccess access;
    private final RubricService rubrics;
    private final DocumentModelService documents;
    private final AssignmentAiDraftService drafts;
    private final IClassAccess classes;
    private final ObjectMapper json;

    public AssignmentMcpTools(AssignmentRepository assignments, AssignmentAccess access, RubricService rubrics,
                              DocumentModelService documents, AssignmentAiDraftService drafts, IClassAccess classes,
                              ObjectMapper json) {
        this.assignments = assignments;
        this.access = access;
        this.rubrics = rubrics;
        this.documents = documents;
        this.drafts = drafts;
        this.classes = classes;
        this.json = json;
    }

    @Override
    public List<McpToolDefinition> tools() {
        return List.of(
                new McpToolDefinition(READ, "Giảng viên: đọc đề bài tập (hướng dẫn, khung dạng chữ, các mục, rubric). "
                        + "Sinh viên: chỉ thông tin bài (hình thức, chế độ, lịch), không có đề và rubric.",
                        "{\"type\":\"object\",\"required\":[\"assignmentId\"],\"properties\":{\"assignmentId\":"
                                + "{\"type\":\"string\",\"format\":\"uuid\"}}}",
                        McpToolKind.READ, Set.of(Role.STUDENT, Role.TEACHER, Role.SUBJECT_MANAGER)),
                new McpToolDefinition(SAVE, "Lưu bản nháp đề (hướng dẫn, các mục, gợi ý rubric) thành đề xuất chờ "
                        + "giảng viên áp dụng; cần expectedVersion của bài.",
                        "{\"type\":\"object\",\"required\":[\"assignmentId\",\"expectedVersion\",\"draft\"],"
                                + "\"properties\":{\"assignmentId\":{\"type\":\"string\",\"format\":\"uuid\"},"
                                + "\"expectedVersion\":{\"type\":\"integer\"},\"agentLabel\":{\"type\":\"string\"},"
                                + "\"draft\":" + AiDraftProposal.OUTPUT_SCHEMA.strip() + "}}",
                        McpToolKind.WRITE, Set.of(Role.TEACHER, Role.SUBJECT_MANAGER)));
    }

    @Override
    public McpToolResult call(Actor actor, String toolName, Map<String, Object> args) {
        boolean educator = actor != null && (actor.role() == Role.TEACHER || actor.role() == Role.SUBJECT_MANAGER);
        if (!educator && !(actor != null && actor.role() == Role.STUDENT && READ.equals(toolName))) {
            return error("TOOL_NOT_FOUND", "Không có quyền dùng tool này");
        }
        try {
            UUID id = UUID.fromString(String.valueOf(args.get("assignmentId")));
            return switch (toolName) {
                case READ -> new McpToolResult(false, read(actor, id));
                case SAVE -> {
                    long expected = Long.parseLong(String.valueOf(args.get("expectedVersion")));
                    JsonNode draft = json.valueToTree(args.get("draft"));
                    Object label = args.get("agentLabel");
                    AssignmentAiDraft saved = drafts.saveFromAgent(actor, id, expected, draft,
                            label == null ? null : String.valueOf(label));
                    yield new McpToolResult(false, Map.of("draftId", saved.id().toString(), "status", "READY"));
                }
                default -> error("TOOL_NOT_FOUND", "Không có tool này");
            };
        } catch (ApiException e) {
            return error(e.code(), e.getMessage());
        } catch (IllegalArgumentException | NullPointerException e) {
            return error("INVALID_ARGUMENTS", "Tham số không hợp lệ");
        }
    }

    private Map<String, Object> read(Actor actor, UUID id) {
        Assignment assignment = assignments.find(id).orElseThrow(() -> access.deny(actor, READ,
                AssignmentAudit.OBJECT_ASSIGNMENT, id, "NOT_FOUND"));
        boolean teacher = actor.role() != Role.STUDENT && access.canRead(actor, assignment);
        if (!teacher && !(actor.role() == Role.STUDENT && studentCanRead(actor, assignment))) {
            throw access.deny(actor, READ, AssignmentAudit.OBJECT_ASSIGNMENT, id, "OUT_OF_ASSIGNMENT_SCOPE");
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("assignmentId", assignment.id().toString());
        out.put("title", assignment.title());
        out.put("status", assignment.status().name());
        out.put("submissionMode", assignment.submissionMode().name());
        out.put("gradingMode", assignment.gradingMode().name());
        if (!teacher) {
            // Người học chỉ thấy thông tin như trang trước khi làm bài: đề nằm trong lượt làm, rubric không lộ
            // (yêu cầu 2026-10-21).
            out.put("requiredDiagrams", assignment.requiredDiagrams());
            putSchedule(out, assignment);
            return out;
        }
        out.put("instructions", assignment.instructions());
        out.put("document", documents.toPlainText(assignment.skeleton()));
        out.put("parts", assignment.parts().stream().map(p -> Map.of("partId", p.partId(), "title", p.title())).toList());
        out.put("requiredDiagrams", assignment.requiredDiagrams());
        out.put("rubrics", rubrics.ordered(assignment, rubrics.all(assignment.id())).stream()
                .map(r -> rubric(r.view())).toList());
        putSchedule(out, assignment);
        out.put("version", assignment.version());
        return out;
    }

    private static void putSchedule(Map<String, Object> out, Assignment assignment) {
        if (assignment.closesAt() != null) {
            out.put("closesAt", assignment.closesAt().toString());
        }
        if (assignment.lateUntil() != null) {
            out.put("lateUntil", assignment.lateUntil().toString());
        }
        if (assignment.maxAttempts() != null) {
            out.put("maxAttempts", assignment.maxAttempts());
        }
    }

    /** Người học đọc bài OPEN, CLOSED của lớp OPEN mình đang học, như Assignment Detail (BR-U07-37). */
    private boolean studentCanRead(Actor actor, Assignment assignment) {
        if (assignment.status() != AssignmentStatus.OPEN && assignment.status() != AssignmentStatus.CLOSED) {
            return false;
        }
        for (ClassRef ref : classes.listOpenClassesOf(actor.accountId())) {
            boolean match = assignment.scope() == AssignmentScope.CLASS ? ref.classId().equals(assignment.classId())
                    : ref.subjectId().equals(assignment.subjectId());
            if (match && ref.status() == ClassStatus.OPEN && classes.isActiveStudent(actor.accountId(), ref.classId())) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, Object> rubric(RubricView view) {
        return Map.of("partId", view.partId(), "maxPoints", view.maxPoints(), "criteria", view.criteria().stream()
                .map(c -> Map.of("title", c.title(), "kind", c.kind().name(), "items", c.items().stream()
                        .map(i -> Map.of("text", i.text(), "points", i.points() == null ? 0 : i.points())).toList()))
                .toList());
    }

    private static McpToolResult error(String code, String message) {
        return new McpToolResult(true, Map.of("code", code, "message", message));
    }
}
