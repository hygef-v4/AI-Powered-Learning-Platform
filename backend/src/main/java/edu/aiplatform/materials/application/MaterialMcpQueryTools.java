package edu.aiplatform.materials.application;

import edu.aiplatform.ai.contract.*;
import edu.aiplatform.identity.contract.Actor;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class MaterialMcpQueryTools implements IMcpToolProvider {
    private final MaterialQueryService queries;
    public MaterialMcpQueryTools(MaterialQueryService queries){this.queries=queries;}
    @Override public List<McpToolDefinition> tools(){return List.of(McpSchemas.read("list_materials","Tìm học liệu được phép; kiểm textStatus trước khi chọn làm nguồn.",
            McpSchemas.SCOPE+","+McpSchemas.PAGE+",\"query\":"+McpSchemas.TEXT));}
    @Override public McpToolResult call(Actor actor,String name,Map<String,Object> args){
        McpInput.teaching(actor);if(!"list_materials".equals(name))throw McpInput.invalid();var in=new McpInput(args);
        int page=in.page(),size=in.size();if(in.has("classId")==in.has("subjectId"))throw McpInput.invalid();
        String query=in.text("query","").toLowerCase(Locale.ROOT);
        var rows=in.has("classId")?queries.classList(actor,in.uuid("classId"),false,null):queries.subjectList(actor,in.uuid("subjectId"));
        var items=rows.stream().filter(m->m.title().toLowerCase(Locale.ROOT).contains(query))
                .map(m->Map.of("materialId",m.id(),"title",m.title(),"kind",m.kind(),"status",m.status(),"versionNo",m.currentVersionNo(),"textStatus",m.current().textStatus())).toList();
        return new McpToolResult(false,McpInput.page(items,page,size));
    }
}
