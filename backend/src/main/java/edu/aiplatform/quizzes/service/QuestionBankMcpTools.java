package edu.aiplatform.quizzes.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.ai.contract.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.quizzes.contract.QuestionType;
import edu.aiplatform.quizzes.model.*;
import edu.aiplatform.quizzes.repository.*;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class QuestionBankMcpTools implements IMcpToolProvider {
    private final QuestionBankService bank;
    private final QuizAiDraftService drafts;
    private final QuizAccess access;
    private final ActiveSubjects active;
    private final IMcpWriteRequest writes;
    private final ObjectMapper json;
    public QuestionBankMcpTools(QuestionBankService bank,QuizAiDraftService drafts,QuizAccess access,
            ActiveSubjects active,IMcpWriteRequest writes,ObjectMapper json) {
        this.bank=bank;this.drafts=drafts;this.access=access;this.active=active;this.writes=writes;this.json=json;
    }
    @Override public List<McpToolDefinition> tools() {
        String subject="\"subjectId\":"+McpSchemas.UUID;
        return List.of(
                definition("read_question_bank_context",McpToolKind.READ,subject+",\"questionId\":"+McpSchemas.UUID),
                definition("list_bank_questions",McpToolKind.READ,subject+","+McpSchemas.PAGE+",\"query\":"+McpSchemas.TEXT
                        +",\"type\":{\"enum\":[\"MCQ_SINGLE\",\"MCQ_MULTI\"]},\"difficulty\":{\"enum\":[\"EASY\",\"MEDIUM\",\"HARD\"]},\"status\":{\"enum\":[\"DRAFT\",\"ACTIVE\",\"RETIRED\"]}"),
                definition("save_question_bank_draft",McpToolKind.WRITE,subject+",\"questions\":"+QuizMcpAuthoringService.questionsSchema(json)
                        +",\"requestRef\":"+McpSchemas.TEXT+",\"agentLabel\":"+McpSchemas.TEXT,"questions","requestRef"));
    }
    private McpToolDefinition definition(String name,McpToolKind kind,String props,String... extra) {
        var required=new ArrayList<String>();required.add("subjectId");required.addAll(List.of(extra));
        return new McpToolDefinition(name,"Ngân hàng câu hỏi của môn phụ trách; ghi chỉ tạo đề xuất chờ duyệt.",
                McpSchemas.schema(props,required.toArray(String[]::new)),kind,Set.of(Role.SUBJECT_MANAGER));
    }
    @Override public McpToolResult call(Actor actor,String name,Map<String,Object> args) {
        if(actor==null||actor.role()!=Role.SUBJECT_MANAGER)throw edu.aiplatform.shared.error.ApiException.notFound("MCP_TOOL_NOT_FOUND");
        var in=new McpInput(args);UUID id=in.uuid("subjectId");
        access.requireSubject(actor,"save_question_bank_draft".equals(name)?Actions.QUESTION_BANK_MANAGE:Actions.QUESTION_BANK_VIEW,
                id,"SUBJECT",id,"SUBJECT_NOT_FOUND");
        return new McpToolResult(false,switch(name) {
            case "read_question_bank_context" -> {
                Map<String,Object> context=new LinkedHashMap<>();context.put("subjectId",id);context.put("questionSchema",readQuestionSchema());
                context.put("workflow","Save proposal; review and accept selected questions in the question bank UI.");
                if(in.has("questionId")) {
                    var question=bank.detail(actor,in.uuid("questionId")).question();
                    if(!id.equals(question.subjectId()))throw edu.aiplatform.shared.error.ApiException.notFound("QUESTION_NOT_FOUND");
                    context.put("question",question);
                }yield context;
            }
            case "list_bank_questions" -> bank.list(actor,id,new QuestionBankService.Filter(in.choice("type",QuestionType.class,null),
                    in.choice("difficulty",Difficulty.class,null),in.choice("status",QuestionStatus.class,null),in.text("query",null),in.page(),in.size()));
            case "save_question_bank_draft" -> {
                active.require(id);
                var payload=new LinkedHashMap<>(args);payload.remove("requestRef");
                yield writes.execute(actor,name,in.text("requestRef",null),McpFingerprint.of(json,payload),() -> {
                    var proposal=drafts.saveBankFromMcp(actor,id,json.valueToTree(in.get("questions")),in.text("agentLabel",null));
                    return Map.of("subjectId",id.toString(),"proposalId",proposal.id().toString(),"status","READY");
                });
            }
            default -> throw McpInput.invalid();
        });
    }
    private Object readQuestionSchema() {
        try {return json.readTree(QuizMcpAuthoringService.questionsSchema(json));}
        catch(Exception e){throw new IllegalStateException(e);}
    }
}
