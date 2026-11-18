package edu.aiplatform.quizzes.service;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.aiplatform.ai.contract.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.quizzes.contract.QuizSettings;
import edu.aiplatform.quizzes.model.Quiz;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class QuizMcpAuthoringService implements IMcpToolProvider {
    private static final String SETTINGS_SCHEMA = "{\"type\":\"object\","
            + "\"additionalProperties\":false,\"properties\":{"
            + "\"shuffleQuestions\":{\"type\":\"boolean\"},\"shuffleOptions\":{\"type\":\"boolean\"},"
            + "\"timeLimitMinutes\":{\"type\":[\"integer\",\"null\"]},"
            + "\"maxAttempts\":{\"type\":[\"integer\",\"null\"]},"
            + "\"showScoreAfterSubmit\":{\"type\":\"boolean\"},"
            + "\"showCorrectAnswers\":{\"type\":\"boolean\"}}}";

    private final QuizAuthoringService quizAuthoringService;
    private final QuizAiDraftService quizAiDraftService;
    private final QuizAccess access;
    private final IMcpWriteRequest writes;
    private final ObjectMapper json;

    public QuizMcpAuthoringService(QuizAuthoringService quizAuthoringService,
                                   QuizAiDraftService quizAiDraftService, QuizAccess access,
                                   IMcpWriteRequest writes, ObjectMapper json) {
        this.quizAuthoringService = quizAuthoringService;
        this.quizAiDraftService = quizAiDraftService;
        this.access = access;
        this.writes = writes;
        this.json = json;
    }

    static String buildQuestionsSchema(ObjectMapper json) {
        try {
            String toolSchema = new QuizMcpTools(null, json).tools().get(1).parameterSchema();
            return json.readTree(toolSchema).at("/properties/questions").toString();
        } catch (Exception e) {
            throw new IllegalStateException("Invalid question schema", e);
        }
    }

    @Override
    public List<McpToolDefinition> tools() {
        String properties = McpSchemas.SCOPE
                + ",\"title\":" + McpSchemas.TEXT
                + ",\"requestRef\":" + McpSchemas.TEXT
                + ",\"instructions\":{\"type\":\"string\",\"maxLength\":10000}"
                + ",\"settings\":" + SETTINGS_SCHEMA
                + ",\"agentLabel\":" + McpSchemas.TEXT
                + ",\"questions\":" + buildQuestionsSchema(json);
        return List.of(new McpToolDefinition("create_quiz_draft",
                "Tạo quiz DRAFT và đề xuất câu hỏi chờ người soạn áp dụng; requestRef chống tạo trùng.",
                McpSchemas.schema(properties, "title", "requestRef", "questions"),
                McpToolKind.WRITE, McpSchemas.STAFF));
    }

    @Override
    public McpToolResult call(Actor actor, String name, Map<String, Object> args) {
        McpInput.requireTeaching(actor);
        if (!"create_quiz_draft".equals(name)) {
            throw McpInput.createInvalidArguments();
        }
        var in = new McpInput(args);
        if (in.has("classId") == in.has("subjectId")) {
            throw McpInput.createInvalidArguments();
        }
        boolean forClass = in.has("classId");
        UUID target = in.readUuid(forClass ? "classId" : "subjectId");
        if (forClass) {
            access.requireClass(actor, Actions.QUIZ_CLASS_MANAGE, target, "CLASS", target,
                    "CLASS_NOT_FOUND");
        } else {
            access.requireSubject(actor, Actions.QUIZ_SUBJECT_MANAGE, target, "SUBJECT", target,
                    "SUBJECT_NOT_FOUND");
        }
        JsonNode questions = json.valueToTree(in.readValue("questions"));
        requireValidQuestions(questions);
        QuizSettings selected = in.has("settings")
                ? mergeSettings(in.readValue("settings"))
                : Quiz.DEFAULT_SETTINGS;
        Map<String, Object> semantic = new LinkedHashMap<>(args);
        semantic.remove("requestRef");
        semantic.put("settings", selected);
        return new McpToolResult(false, writes.execute(actor, name, in.readText("requestRef", null),
                McpFingerprint.of(json, semantic), () -> {
                    String title = in.readText("title", null);
                    var created = forClass
                            ? quizAuthoringService.createForClass(actor, target, title)
                            : quizAuthoringService.createForSubject(actor, target, title);
                    var details = new QuizAuthoringService.Details(title,
                            in.readText("instructions", null), selected, false, null);
                    var changed = quizAuthoringService.updateDetails(actor, created.quiz().id(),
                            details, created.quiz().version());
                    var proposal = quizAiDraftService.saveFromMcp(actor, changed.quiz().id(),
                            changed.quiz().version(), questions, in.readText("agentLabel", null));
                    return Map.of("quizId", changed.quiz().id().toString(),
                            "proposalId", proposal.id().toString(),
                            "version", changed.quiz().version(),
                            "status", "DRAFT",
                            "proposalStatus", "READY");
                }));
    }

    private static void requireValidQuestions(JsonNode questions) {
        try {
            if (QuizDraftPrompt.parseQuestions(questions).stream()
                    .anyMatch(question -> !question.violations().isEmpty())) {
                throw McpInput.createInvalidArguments();
            }
        } catch (IllegalArgumentException e) {
            throw McpInput.createInvalidArguments();
        }
    }

    private QuizSettings mergeSettings(Object providedValue) {
        ObjectNode merged = json.valueToTree(Quiz.DEFAULT_SETTINGS);
        JsonNode provided = json.valueToTree(providedValue);
        if (!provided.isObject()) {
            throw McpInput.createInvalidArguments();
        }
        provided.fields().forEachRemaining(field -> merged.set(field.getKey(), field.getValue()));
        try {
            return json.treeToValue(merged, QuizSettings.class);
        } catch (Exception e) {
            throw McpInput.createInvalidArguments();
        }
    }
}
