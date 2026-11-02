package edu.aiplatform.quizzes.application;

import edu.aiplatform.ai.contract.*;
import edu.aiplatform.quizzes.contract.QuizStatus;
import edu.aiplatform.identity.contract.Actor;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class QuizMcpQueryTools implements IMcpToolProvider {
    private final QuizAuthoringService authoring;
    public QuizMcpQueryTools(QuizAuthoringService authoring){this.authoring=authoring;}
    @Override public List<McpToolDefinition> tools(){return List.of(McpSchemas.read("list_quizzes","Tìm quiz theo lớp hoặc môn được phép.",
            McpSchemas.SCOPE+","+McpSchemas.PAGE+",\"query\":"+McpSchemas.TEXT+",\"status\":{\"enum\":[\"DRAFT\",\"REVIEWED\",\"OPEN\",\"RETIRED\"]}"));}
    @Override public McpToolResult call(Actor actor,String name,Map<String,Object> args){
        McpInput.teaching(actor);if(!"list_quizzes".equals(name))throw McpInput.invalid();var in=new McpInput(args);
        int page=in.page(),size=in.size();if(in.has("classId")==in.has("subjectId"))throw McpInput.invalid();
        var state=in.choice("status",QuizStatus.class,null);String query=in.text("query","").toLowerCase(Locale.ROOT);
        if(in.has("subjectId"))return new McpToolResult(false,authoring.listSubject(actor,in.uuid("subjectId"),state,query,page,size));
        var rows=authoring.listClass(actor,in.uuid("classId")).stream().filter(q->state==null||q.status()==state)
                .filter(q->q.title().toLowerCase(Locale.ROOT).contains(query)).toList();
        return new McpToolResult(false,McpInput.page(rows,page,size));
    }
}
