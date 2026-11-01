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
        return "Start and clarify the task in this external agent chat; no platform form is required. Resolve authorized targets first. "
                +"If multiple classes/assignments match or material requirements are missing, ask one grouped clarification before writing; never guess IDs, scale or scope. "
                +"For authoring call read_authoring_requirements; reuse explicit assignment settings when revising. "
                +"For grading select only the agreed PENDING/DRAFT roots, read all read_grading_context continuations and pass the snapshotFingerprint on writes. "
                +"MCP_CONTEXT_READ_REQUIRED means reread from offset 0; context changes require reassessment. "
                +"Return the exact reviewUrl from each successful tool result. The platform UI is for viewing/editing/approval; never regenerate to open a review or publish official grades.";
    }
}
