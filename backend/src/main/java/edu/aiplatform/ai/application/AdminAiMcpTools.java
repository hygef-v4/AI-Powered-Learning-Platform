package edu.aiplatform.ai.application;

import edu.aiplatform.ai.contract.*;
import edu.aiplatform.identity.contract.*;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class AdminAiMcpTools implements IMcpToolProvider {
    private final QuotaPolicyService quota;
    private final AiConnectionService connections;
    public AdminAiMcpTools(QuotaPolicyService quota,AiConnectionService connections) {this.quota=quota;this.connections=connections;}
    @Override public List<McpToolDefinition> tools() {
        return List.of(read("read_ai_usage_summary",McpSchemas.PAGE+",\"days\":{\"enum\":[7,30]}"),
                read("read_ai_quota_policy",McpSchemas.PAGE),read("read_ai_connection_status",""));
    }
    private McpToolDefinition read(String name,String properties) {
        return new McpToolDefinition(name,"Báo cáo AI chỉ đọc cho Admin, không có khóa API hay token.",McpSchemas.schema(properties),McpToolKind.READ,Set.of(Role.ADMIN));
    }
    @Override public McpToolResult call(Actor actor,String name,Map<String,Object> args) {
        McpInput.admin(actor);var in=new McpInput(args);
        return new McpToolResult(false,switch(name) {
            case "read_ai_connection_status" -> Map.of("connections",connections.listInstitutional(actor));
            case "read_ai_quota_policy" -> Map.of("policy",quota.policy(actor),"teacherQuotas",McpInput.page(quota.teacherQuotas(actor),in.page(),in.size()));
            case "read_ai_usage_summary" -> {
                int days=in.integer("days",7,7,30);if(days!=7&&days!=30)throw McpInput.invalid();
                var summary=quota.summary(actor,days);
                Map<String,Object> out=new LinkedHashMap<>();out.put("days",summary.days());out.put("todayTokens",summary.todayTokens());
                out.put("globalDayTokens",summary.globalDayTokens());out.put("periodTokens",summary.periodTokens());out.put("teachersUsed",summary.teachersUsed());
                out.put("byDay",summary.byDay());out.put("byTeacher",McpInput.page(summary.byTeacher(),in.page(),in.size()));out.put("byModel",summary.byModel());yield out;
            }
            default -> throw McpInput.invalid();
        });
    }
}
