package edu.aiplatform.assignments.service;

import edu.aiplatform.ai.contract.*;
import edu.aiplatform.assignments.contract.*;
import edu.aiplatform.identity.contract.Actor;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class AssignmentMcpQueryTools implements IMcpToolProvider {
    private final AssignmentAuthoringService authoring;
    public AssignmentMcpQueryTools(AssignmentAuthoringService authoring){this.authoring=authoring;}
    @Override public List<McpToolDefinition> tools(){return List.of(McpSchemas.read("list_assignments",
            "Tìm bài tập được phép của một lớp hoặc môn. Chọn đúng một classId hoặc subjectId; lọc OPEN cho bài đã giao đang mở.",
            McpSchemas.SCOPE+","+McpSchemas.PAGE+",\"query\":"+McpSchemas.TEXT+",\"status\":{\"enum\":[\"DRAFT\",\"REVIEWED\",\"SCHEDULED\",\"OPEN\",\"CLOSED\",\"RETIRED\"]},\"gradingMode\":{\"enum\":[\"PRACTICE\",\"GRADED\"]}"));}
    @Override public McpToolResult call(Actor actor,String name,Map<String,Object> args){
        McpInput.teaching(actor);if(!"list_assignments".equals(name))throw McpInput.invalid();
        var in=new McpInput(args);int page=in.page(),size=in.size();if(in.has("classId")==in.has("subjectId"))throw McpInput.invalid();
        var state=in.choice("status",AssignmentStatus.class,null);Set<AssignmentStatus> states=state==null?Set.of():Set.of(state);
        var mode=in.choice("gradingMode",GradingMode.class,null);
        var result=in.has("classId")?authoring.listForClass(actor,in.uuid("classId"),states,true,in.text("query",null),mode)
                :authoring.listForSubject(actor,in.uuid("subjectId"),states,true,in.text("query",null),mode);
        return new McpToolResult(false,McpInput.page(result.items(),page,size));
    }
}
