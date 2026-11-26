package edu.aiplatform.assignments.service;

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
import edu.aiplatform.assignments.model.Assignment;
import edu.aiplatform.assignments.model.AssignmentAiDraft;
import edu.aiplatform.assignments.repository.AssignmentRepository;
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
    private final RubricService rubricService;
    private final DocumentModelService documentModelService;
    private final AssignmentAiDraftService assignmentAiDraftService;
    private final IClassAccess classes;
    private final ObjectMapper json;

    // Kết nối dữ liệu đề, rubric, quyền xem và đường lưu đề xuất dùng trong MCP.
    public AssignmentMcpTools(AssignmentRepository assignments, AssignmentAccess access, RubricService rubricService,
                              DocumentModelService documentModelService, AssignmentAiDraftService assignmentAiDraftService, IClassAccess classes,
                              ObjectMapper json) {
        this.assignments = assignments;
        this.access = access;
        this.rubricService = rubricService;
        this.documentModelService = documentModelService;
        this.assignmentAiDraftService = assignmentAiDraftService;
        this.classes = classes;
        this.json = json;
    }

    // Công bố hợp đồng đọc ngữ cảnh và lưu đề xuất theo từng vai trò.
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

    // Phân nhánh đọc bài với lưu đề xuất, trả lỗi tool có mã khi đầu vào hoặc quyền không hợp lệ.
    @Override
    public McpToolResult call(Actor actor, String toolName, Map<String, Object> args) {
        boolean educator = actor != null && (actor.role() == Role.TEACHER || actor.role() == Role.SUBJECT_MANAGER);
        if (!educator && !(actor != null && actor.role() == Role.STUDENT && READ.equals(toolName))) {
            return buildError("TOOL_NOT_FOUND", "Không có quyền dùng tool này");
        }
        try {
            UUID id = UUID.fromString(String.valueOf(args.get("assignmentId")));
            return switch (toolName) {
                case READ -> new McpToolResult(false, readAssignment(actor, id));
                case SAVE -> {
                    // Dùng version từ lần đọc context để phát hiện bài đã đổi trong lúc agent soạn.
                    long expected = Long.parseLong(String.valueOf(args.get("expectedVersion")));
                    JsonNode draft = json.valueToTree(args.get("draft"));
                    Object label = args.get("agentLabel");
                    AssignmentAiDraft saved = assignmentAiDraftService.saveFromAgent(actor, id, expected, draft,
                            label == null ? null : String.valueOf(label));
                    yield new McpToolResult(false, Map.of("draftId", saved.id().toString(), "proposalId",saved.id().toString(),
                            "assignmentId",id.toString(),"status", "READY",
                            "reviewUrl","/assignments/"+id+"?proposalId="+saved.id()));
                }
                default -> buildError("TOOL_NOT_FOUND", "Không có tool này");
            };
        } catch (ApiException e) {
            return buildError(e.readCode(), e.getMessage());
        } catch (IllegalArgumentException | NullPointerException e) {
            return buildError("INVALID_ARGUMENTS", "Tham số không hợp lệ");
        }
    }

    // Chỉ gửi đề, rubric và version đầy đủ khi người dùng có quyền soạn hoặc dạy bài.
    private Map<String, Object> readAssignment(Actor actor, UUID id) {
        Assignment assignment = assignments.findAssignment(id).orElseThrow(() -> access.notFound());
        boolean teacher = actor.role() != Role.STUDENT && access.canRead(actor, assignment);
        if (!teacher && !(actor.role() == Role.STUDENT && canStudentRead(actor, assignment))) {
            throw access.notFound();
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
        out.put("document", documentModelService.toPlainText(assignment.skeleton()));
        out.put("parts", assignment.parts().stream().map(p -> Map.of("partId", p.partId(), "title", p.title())).toList());
        out.put("requiredDiagrams", assignment.requiredDiagrams());
        out.put("rubrics", rubricService.orderRubrics(assignment, rubricService.listRubricModels(assignment.id())).stream()
                .map(r -> mapRubric(r.view())).toList());
        putSchedule(out, assignment);
        out.put("version", assignment.version());
        return out;
    }

    // Bổ sung các thời điểm đã xác định vào kết quả ngữ cảnh bài.
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
    private boolean canStudentRead(Actor actor, Assignment assignment) {
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

    // Chuyển rubric sang dữ liệu JSON gọn theo mục, tiêu chí và điểm.
    private static Map<String, Object> mapRubric(RubricView view) {
        return Map.of("partId", view.partId(), "maxPoints", view.maxPoints(), "criteria", view.criteria().stream()
                .map(c -> Map.of("title", c.title(), "kind", c.kind().name(), "items", c.items().stream()
                        .map(i -> Map.of("text", i.text(), "points", i.points() == null ? 0 : i.points())).toList()))
                .toList());
    }

    // Trả mã lỗi nghiệp vụ trong phản hồi tool để agent xử lý được.
    private static McpToolResult buildError(String code, String message) {
        return new McpToolResult(true, Map.of("code", code, "message", message));
    }
}
