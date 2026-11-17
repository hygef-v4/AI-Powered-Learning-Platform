package edu.aiplatform.assignments.service;

import com.fasterxml.jackson.databind.*;
import edu.aiplatform.ai.contract.*;
import edu.aiplatform.assignments.contract.*;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.shared.error.ApiException;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class AssignmentMcpAuthoringService implements IMcpToolProvider {
    private final AssignmentAuthoringService assignmentAuthoringService;
    private final AssignmentAiDraftService assignmentAiDraftService;
    private final AssignmentAccess access;
    private final IMcpWriteRequest writes;
    private final ObjectMapper json;
    public AssignmentMcpAuthoringService(AssignmentAuthoringService assignmentAuthoringService,AssignmentAiDraftService assignmentAiDraftService,
            AssignmentAccess access,IMcpWriteRequest writes,ObjectMapper json) {
        this.assignmentAuthoringService=assignmentAuthoringService;this.assignmentAiDraftService=assignmentAiDraftService;this.access=access;this.writes=writes;this.json=json;
    }
    @Override public List<McpToolDefinition> tools() {
        return List.of(new McpToolDefinition("create_assignment_draft","Đọc read_agent_workflow với workflow=AUTHOR_ASSIGNMENT trước khi soạn. Tạo bài tập DRAFT kèm đề xuất để người soạn xem và áp dụng. requestRef chống tạo trùng.",
                McpSchemas.schema(McpSchemas.SCOPE+",\"title\":"+McpSchemas.TEXT
                        +",\"submissionMode\":{\"enum\":[\"INDIVIDUAL\",\"GROUP\"]},\"gradingMode\":{\"enum\":[\"PRACTICE\",\"GRADED\"]}"
                        +",\"referenceMaterialIds\":{\"type\":\"array\",\"maxItems\":20,\"items\":"+McpSchemas.UUID+"}"
                        +",\"requestRef\":"+McpSchemas.TEXT+",\"agentLabel\":"+McpSchemas.TEXT+",\"draft\":"+AiDraftProposal.OUTPUT_SCHEMA.strip(),
                        "title","requestRef","draft"),McpToolKind.WRITE,McpSchemas.STAFF));
    }
    @Override public McpToolResult call(Actor actor,String name,Map<String,Object> args) {
        McpInput.teaching(actor);if(!"create_assignment_draft".equals(name))throw McpInput.invalid();
        var in=new McpInput(args);if(in.has("classId")==in.has("subjectId"))throw McpInput.invalid();
        UUID target=in.uuid(in.has("classId")?"classId":"subjectId");
        // Recheck access on every retry before retrieving any previous IDs.
        if(in.has("classId"))access.requireClassManager(actor,target,"ASSIGNMENT_CREATED");
        else access.requireSubjectManager(actor,target,"ASSIGNMENT_CREATED");
        var submission=in.choice("submissionMode",SubmissionMode.class,SubmissionMode.INDIVIDUAL);
        var grading=in.choice("gradingMode",GradingMode.class,GradingMode.GRADED);
        JsonNode raw=json.valueToTree(in.get("draft"));
        var validation=AssignmentDraftValidator.validate(raw,grading,AssignmentDraftValidator.Options.createLegacyOptions());
        if("INVALID".equals(validation.path("status").asText()))
            throw ApiException.createBadRequest("AI_DRAFT_INVALID","Nội dung hoặc rubric không hợp lệ");
        List<UUID> materials=parseIds(in.get("referenceMaterialIds"));
        Map<String,Object> semantic=new LinkedHashMap<>(args);semantic.remove("requestRef");
        semantic.put("submissionMode",submission.name());semantic.put("gradingMode",grading.name());
        semantic.put("referenceMaterialIds",materials);
        var response=writes.execute(actor,name,in.text("requestRef",null),McpFingerprint.of(json,semantic),() -> {
            var command=new AssignmentCommands.Create(submission,grading,true,in.text("title",null));
            var created=in.has("classId")?assignmentAuthoringService.createForClass(actor,target,command):assignmentAuthoringService.createForSubject(actor,target,command);
            if(!materials.isEmpty())created=assignmentAuthoringService.updateDetails(actor,created.id(),new AssignmentCommands.Details(
                    created.version(),null,null,null,null,null,materials,null,null));
            var proposal=assignmentAiDraftService.saveFromAgent(actor,created.id(),created.version(),raw,in.text("agentLabel",null));
            return Map.of("assignmentId",created.id().toString(),"proposalId",proposal.id().toString(),
                    "version",created.version(),"status","DRAFT","proposalStatus","READY",
                    "reviewUrl","/assignments/"+created.id()+"?proposalId="+proposal.id());
        });
        return new McpToolResult(false,response);
    }
    private static List<UUID> parseIds(Object value) {
        if(value==null)return List.of();if(!(value instanceof List<?> list)||list.size()>20)throw McpInput.invalid();
        return list.stream().map(item -> new McpInput(Map.of("id",item)).uuid("id")).distinct().toList();
    }
}
