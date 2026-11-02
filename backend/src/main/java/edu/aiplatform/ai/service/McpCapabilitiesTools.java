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
        return new McpToolResult(false,Map.of("role",actor.role(),"workflow",workflow(actor.role())));
    }
    static String workflow(Role role) {
        if(role==Role.STUDENT)return "Start in agent chat. Only submitted own Practice work is eligible for private feedback; no official grading or publication.";
        return "Start and clarify the task in this external agent chat; no platform form or local skill installation is required. "
                +"Before authoring assignments call read_agent_workflow with workflow=AUTHOR_ASSIGNMENT; "
                +"before grading call it with workflow=GRADE_WITH_RUBRIC. Follow the complete server-managed instructions and current domain tool schemas. "
                +"For quiz/question-bank authoring call read_authoring_requirements with the matching kind and clarify missing requirements before writing. "
                +"Return the exact reviewUrl for platform UI editing/approval. Never publish official grades through MCP.";
    }
}
