package edu.aiplatform.quizzes.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.ai.contract.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.quizzes.contract.QuestionType;
import edu.aiplatform.quizzes.model.*;
import edu.aiplatform.shared.error.ApiException;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class QuestionBankMcpTools implements IMcpToolProvider {
    private final QuestionBankService questionBankService;
    private final QuizAiDraftService quizAiDraftService;
    private final QuizAccess access;
    private final ActiveSubjects active;
    private final IMcpWriteRequest writes;
    private final ObjectMapper json;

    public QuestionBankMcpTools(QuestionBankService questionBankService,
                                QuizAiDraftService quizAiDraftService, QuizAccess access,
                                ActiveSubjects active, IMcpWriteRequest writes, ObjectMapper json) {
        this.questionBankService = questionBankService;
        this.quizAiDraftService = quizAiDraftService;
        this.access = access;
        this.active = active;
        this.writes = writes;
        this.json = json;
    }

    @Override
    public List<McpToolDefinition> tools() {
        String subject = "\"subjectId\":" + McpSchemas.UUID;
        return List.of(
                defineTool("read_question_bank_context", McpToolKind.READ,
                        subject + ",\"questionId\":" + McpSchemas.UUID),
                defineTool("list_bank_questions", McpToolKind.READ,
                        subject + "," + McpSchemas.PAGE + ",\"query\":" + McpSchemas.TEXT
                                + ",\"type\":{\"enum\":[\"MCQ_SINGLE\",\"MCQ_MULTI\"]}"
                                + ",\"difficulty\":{\"enum\":[\"EASY\",\"MEDIUM\",\"HARD\"]}"
                                + ",\"status\":{\"enum\":[\"DRAFT\",\"PENDING\",\"REJECTED\","
                                + "\"ACTIVE\",\"RETIRED\"]}"),
                defineTool("save_question_bank_draft", McpToolKind.WRITE,
                        subject + ",\"questions\":" + QuizMcpAuthoringService.buildQuestionsSchema(json)
                                + ",\"requestRef\":" + McpSchemas.TEXT
                                + ",\"agentLabel\":" + McpSchemas.TEXT,
                        "questions", "requestRef"));
    }

    private McpToolDefinition defineTool(String name, McpToolKind kind, String props,
                                         String... extra) {
        var required = new ArrayList<String>();
        required.add("subjectId");
        required.addAll(List.of(extra));
        return new McpToolDefinition(name,
                "Ngân hàng câu hỏi của môn phụ trách; ghi chỉ tạo đề xuất chờ duyệt.",
                McpSchemas.schema(props, required.toArray(String[]::new)), kind,
                Set.of(Role.SUBJECT_MANAGER));
    }

    @Override
    public McpToolResult call(Actor actor, String name, Map<String, Object> args) {
        if (actor == null || actor.role() != Role.SUBJECT_MANAGER) {
            throw ApiException.createNotFound("MCP_TOOL_NOT_FOUND");
        }
        var in = new McpInput(args);
        UUID id = in.readUuid("subjectId");
        String action = "save_question_bank_draft".equals(name)
                ? Actions.QUESTION_BANK_MANAGE
                : Actions.QUESTION_BANK_VIEW;
        access.requireSubject(actor, action, id, "SUBJECT", id, "SUBJECT_NOT_FOUND");
        return new McpToolResult(false, switch (name) {
            case "read_question_bank_context" -> loadContext(actor, id, in);
            case "list_bank_questions" -> questionBankService.listQuestions(actor, id,
                    new QuestionBankService.Filter(
                            in.readChoice("type", QuestionType.class, null),
                            in.readChoice("difficulty", Difficulty.class, null),
                            in.readChoice("status", QuestionStatus.class, null),
                            in.readText("query", null), in.readPage(), in.readSize()));
            case "save_question_bank_draft" -> saveDraft(actor, name, id, in, args);
            default -> throw McpInput.createInvalidArguments();
        });
    }

    private Map<String, Object> loadContext(Actor actor, UUID subjectId, McpInput in) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("subjectId", subjectId);
        context.put("questionSchema", readQuestionSchema());
        context.put("workflow",
                "Save proposal; review and accept selected questions in the question bank UI.");
        if (in.has("questionId")) {
            var question = questionBankService.loadQuestion(actor, in.readUuid("questionId"))
                    .question();
            if (!subjectId.equals(question.subjectId())) {
                throw ApiException.createNotFound("QUESTION_NOT_FOUND");
            }
            context.put("question", question);
        }
        return context;
    }

    private Map<String, Object> saveDraft(Actor actor, String name, UUID subjectId, McpInput in,
                                          Map<String, Object> args) {
        active.require(subjectId);
        var payload = new LinkedHashMap<>(args);
        payload.remove("requestRef");
        return writes.execute(actor, name, in.readText("requestRef", null),
                McpFingerprint.of(json, payload), () -> {
                    var proposal = quizAiDraftService.saveBankFromMcp(actor, subjectId,
                            json.valueToTree(in.readValue("questions")),
                            in.readText("agentLabel", null));
                    return Map.of("subjectId", subjectId.toString(),
                            "proposalId", proposal.id().toString(),
                            "status", "READY");
                });
    }

    private Object readQuestionSchema() {
        try {
            return json.readTree(QuizMcpAuthoringService.buildQuestionsSchema(json));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
