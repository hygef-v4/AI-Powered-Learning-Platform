package edu.aiplatform.ai.service;

import edu.aiplatform.ai.contract.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.shared.error.ApiException;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class McpCapabilitiesTools implements IMcpToolProvider {
    @Override public List<McpToolDefinition> tools(){return List.of(new McpToolDefinition("get_my_capabilities",
            "Role hiện tại, quyền token và các tool được phép. Quyền đối tượng được kiểm tra khi gọi từng tool.",
            McpSchemas.schema(""),McpToolKind.READ,Set.of(Role.STUDENT,Role.TEACHER,Role.SUBJECT_MANAGER)));}
    @Override public McpToolResult call(Actor actor,String name,Map<String,Object> args){
        if(actor==null||actor.role()==null||actor.role()==Role.ADMIN||!"get_my_capabilities".equals(name))throw ApiException.notFound("MCP_TOOL_NOT_FOUND");
        return new McpToolResult(false,Map.of("role",actor.role()));
    }
}
