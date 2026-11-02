package edu.aiplatform.quizzes.application;

import com.fasterxml.jackson.databind.*;
import edu.aiplatform.ai.contract.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.quizzes.contract.QuizSettings;
import edu.aiplatform.quizzes.domain.Quiz;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class QuizMcpAuthoringService implements IMcpToolProvider {
    private final QuizAuthoringService authoring;
    private final QuizAiDraftService drafts;
    private final QuizAccess access;
    private final IMcpWriteRequest writes;
    private final ObjectMapper json;
    public QuizMcpAuthoringService(QuizAuthoringService authoring,QuizAiDraftService drafts,QuizAccess access,
            IMcpWriteRequest writes,ObjectMapper json) {
        this.authoring=authoring;this.drafts=drafts;this.access=access;this.writes=writes;this.json=json;
    }
    static String questionsSchema(ObjectMapper json) {
        try { return json.readTree(new QuizMcpTools(null,json).tools().get(1).parameterSchema()).at("/properties/questions").toString(); }
        catch(Exception e){throw new IllegalStateException("Invalid question schema",e);}
    }
    @Override public List<McpToolDefinition> tools() {
        return List.of(new McpToolDefinition("create_quiz_draft","Tạo quiz DRAFT và đề xuất câu hỏi chờ người soạn áp dụng; requestRef chống tạo trùng.",
                McpSchemas.schema(McpSchemas.SCOPE+",\"title\":"+McpSchemas.TEXT+",\"requestRef\":"+McpSchemas.TEXT
                        +",\"instructions\":{\"type\":\"string\",\"maxLength\":10000},\"settings\":{\"type\":\"object\",\"additionalProperties\":false,\"properties\":{"
                        +"\"shuffleQuestions\":{\"type\":\"boolean\"},\"shuffleOptions\":{\"type\":\"boolean\"},"
                        +"\"timeLimitMinutes\":{\"type\":[\"integer\",\"null\"]},\"maxAttempts\":{\"type\":[\"integer\",\"null\"]},"
                        +"\"showScoreAfterSubmit\":{\"type\":\"boolean\"},\"showCorrectAnswers\":{\"type\":\"boolean\"}}}"
                        +",\"referenceMaterialIds\":{\"type\":\"array\",\"maxItems\":20,\"items\":"+McpSchemas.UUID+"}"
                        +",\"agentLabel\":"+McpSchemas.TEXT+",\"questions\":"+questionsSchema(json),"title","requestRef","questions"),McpToolKind.WRITE,McpSchemas.STAFF));
    }
    @Override public McpToolResult call(Actor actor,String name,Map<String,Object> args) {
        McpInput.teaching(actor);if(!"create_quiz_draft".equals(name))throw McpInput.invalid();var in=new McpInput(args);
        if(in.has("classId")==in.has("subjectId"))throw McpInput.invalid();
        UUID target=in.uuid(in.has("classId")?"classId":"subjectId");
        if(in.has("classId"))access.requireClass(actor,Actions.QUIZ_CLASS_MANAGE,target,"CLASS",target,"CLASS_NOT_FOUND");
        else access.requireSubject(actor,Actions.QUIZ_SUBJECT_MANAGE,target,"SUBJECT",target,"SUBJECT_NOT_FOUND");
        JsonNode questions=json.valueToTree(in.get("questions"));
        try {
            if(QuizDraftPrompt.parseQuestions(questions).stream().anyMatch(q -> !q.violations().isEmpty()))throw McpInput.invalid();
        } catch(IllegalArgumentException e) {throw McpInput.invalid();}
        QuizSettings settings=Quiz.DEFAULT_SETTINGS;
        if(in.has("settings")) {
            var merged=json.valueToTree(Quiz.DEFAULT_SETTINGS);JsonNode provided=json.valueToTree(in.get("settings"));
            if(!provided.isObject())throw McpInput.invalid();provided.fields().forEachRemaining(e -> ((com.fasterxml.jackson.databind.node.ObjectNode)merged).set(e.getKey(),e.getValue()));
            try{settings=json.treeToValue(merged,QuizSettings.class);}catch(Exception e){throw McpInput.invalid();}
        }
        List<UUID> materials=List.of();if(in.has("referenceMaterialIds")) {
            if(!(in.get("referenceMaterialIds") instanceof List<?> list)||list.size()>20)throw McpInput.invalid();
            materials=list.stream().map(v -> new McpInput(Map.of("id",v)).uuid("id")).distinct().toList();
        }
        final var chosen=materials;final var selected=settings;
        Map<String,Object> semantic=new LinkedHashMap<>(args);semantic.remove("requestRef");semantic.put("settings",selected);semantic.put("referenceMaterialIds",chosen);
        return new McpToolResult(false,writes.execute(actor,name,in.text("requestRef",null),McpFingerprint.of(json,semantic),() -> {
            var created=in.has("classId")?authoring.createForClass(actor,target,in.text("title",null)):authoring.createForSubject(actor,target,in.text("title",null));
            var changed=authoring.updateDetails(actor,created.quiz().id(),new QuizAuthoringService.Details(in.text("title",null),
                    in.text("instructions",null),selected,false,null,chosen),created.quiz().version());
            var proposal=drafts.saveFromMcp(actor,changed.quiz().id(),changed.quiz().version(),questions,in.text("agentLabel",null));
            return Map.of("quizId",changed.quiz().id().toString(),"proposalId",proposal.id().toString(),"version",changed.quiz().version(),"status","DRAFT","proposalStatus","READY");
        }));
    }
}
