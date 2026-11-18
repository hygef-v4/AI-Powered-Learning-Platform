package edu.aiplatform.assignments.service;

import edu.aiplatform.ai.contract.*;
import edu.aiplatform.assignments.contract.*;
import edu.aiplatform.identity.contract.Actor;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class AssignmentMcpQueryTools implements IMcpToolProvider {
    private final AssignmentAuthoringService assignmentAuthoringService;

    public AssignmentMcpQueryTools(AssignmentAuthoringService assignmentAuthoringService) {
        this.assignmentAuthoringService = assignmentAuthoringService;
    }

    @Override
    public List<McpToolDefinition> tools() {
        String properties = McpSchemas.SCOPE + "," + McpSchemas.PAGE
                + ",\"query\":" + McpSchemas.TEXT
                + ",\"status\":{\"enum\":[\"DRAFT\",\"REVIEWED\",\"SCHEDULED\",\"OPEN\","
                + "\"CLOSED\",\"RETIRED\"]}"
                + ",\"gradingMode\":{\"enum\":[\"PRACTICE\",\"GRADED\"]}";
        return List.of(McpSchemas.read("list_assignments",
                "Tìm bài tập được phép của một lớp hoặc môn. Chọn đúng một classId hoặc "
                        + "subjectId; lọc OPEN cho bài đã giao đang mở.",
                properties));
    }

    @Override
    public McpToolResult call(Actor actor, String name, Map<String, Object> args) {
        McpInput.requireTeaching(actor);
        if (!"list_assignments".equals(name)) {
            throw McpInput.createInvalidArguments();
        }
        var in = new McpInput(args);
        int page = in.readPage();
        int size = in.readSize();
        if (in.has("classId") == in.has("subjectId")) {
            throw McpInput.createInvalidArguments();
        }
        var state = in.readChoice("status", AssignmentStatus.class, null);
        Set<AssignmentStatus> states = state == null ? Set.of() : Set.of(state);
        var mode = in.readChoice("gradingMode", GradingMode.class, null);
        String query = in.readText("query", null);
        var result = in.has("classId")
                ? assignmentAuthoringService.listForClass(actor, in.readUuid("classId"), states,
                        true, query, mode)
                : assignmentAuthoringService.listForSubject(actor, in.readUuid("subjectId"), states,
                        true, query, mode);
        return new McpToolResult(false, McpInput.paginateItems(result.items(), page, size));
    }
}
