package edu.aiplatform.quizzes.service;

import edu.aiplatform.ai.contract.*;
import edu.aiplatform.quizzes.contract.QuizStatus;
import edu.aiplatform.identity.contract.Actor;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class QuizMcpQueryTools implements IMcpToolProvider {
    private final QuizAuthoringService quizAuthoringService;

    public QuizMcpQueryTools(QuizAuthoringService quizAuthoringService) {
        this.quizAuthoringService = quizAuthoringService;
    }

    @Override
    public List<McpToolDefinition> tools() {
        return List.of(McpSchemas.read("list_quizzes", "Tìm quiz theo lớp hoặc môn được phép.",
                McpSchemas.SCOPE + "," + McpSchemas.PAGE + ",\"query\":" + McpSchemas.TEXT
                        + ",\"status\":{\"enum\":[\"DRAFT\",\"OPEN\",\"RETIRED\"]}"));
    }

    @Override
    public McpToolResult call(Actor actor, String name, Map<String, Object> args) {
        McpInput.requireTeaching(actor);
        if (!"list_quizzes".equals(name)) {
            throw McpInput.createInvalidArguments();
        }
        var in = new McpInput(args);
        int page = in.readPage();
        int size = in.readSize();
        if (in.has("classId") == in.has("subjectId")) {
            throw McpInput.createInvalidArguments();
        }
        var state = in.readChoice("status", QuizStatus.class, null);
        String query = in.readText("query", "").toLowerCase(Locale.ROOT);
        if (in.has("subjectId")) {
            return new McpToolResult(false, quizAuthoringService.listSubject(actor,
                    in.readUuid("subjectId"), state, query, page, size));
        }
        var rows = quizAuthoringService.listClass(actor, in.readUuid("classId")).stream()
                .filter(quiz -> state == null || quiz.status() == state)
                .filter(quiz -> quiz.title().toLowerCase(Locale.ROOT).contains(query))
                .toList();
        return new McpToolResult(false, McpInput.paginateItems(rows, page, size));
    }
}
