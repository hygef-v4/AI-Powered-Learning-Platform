package edu.aiplatform.ai.service;

import edu.aiplatform.ai.contract.*;
import edu.aiplatform.identity.contract.*;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class AdminAiMcpTools implements IMcpToolProvider {
    private final QuotaPolicyService quotaPolicyService;
    private final AiConnectionService aiConnectionService;

    public AdminAiMcpTools(QuotaPolicyService quotaPolicyService,
                           AiConnectionService aiConnectionService) {
        this.quotaPolicyService = quotaPolicyService;
        this.aiConnectionService = aiConnectionService;
    }

    @Override
    public List<McpToolDefinition> tools() {
        return List.of(
                defineReadTool("read_ai_usage_summary",
                        McpSchemas.PAGE + ",\"days\":{\"enum\":[7,30]}"),
                defineReadTool("read_ai_quota_policy", McpSchemas.PAGE),
                defineReadTool("read_ai_connection_status", ""));
    }

    private McpToolDefinition defineReadTool(String name, String properties) {
        return new McpToolDefinition(name,
                "Báo cáo AI chỉ đọc cho Admin, không có khóa API hay token.",
                McpSchemas.schema(properties), McpToolKind.READ, Set.of(Role.ADMIN));
    }

    @Override
    public McpToolResult call(Actor actor, String name, Map<String, Object> args) {
        McpInput.requireAdmin(actor);
        var in = new McpInput(args);
        return new McpToolResult(false, switch (name) {
            case "read_ai_connection_status" ->
                    Map.of("connections", aiConnectionService.listInstitutional(actor));
            case "read_ai_quota_policy" -> Map.of(
                    "policy", quotaPolicyService.loadPolicy(actor),
                    "teacherQuotas", McpInput.paginateItems(
                            quotaPolicyService.listTeacherQuotas(actor), in.readPage(), in.readSize()));
            case "read_ai_usage_summary" -> summarizeUsage(actor, in);
            default -> throw McpInput.createInvalidArguments();
        });
    }

    private Map<String, Object> summarizeUsage(Actor actor, McpInput in) {
        int days = in.readInteger("days", 7, 7, 30);
        if (days != 7 && days != 30) {
            throw McpInput.createInvalidArguments();
        }
        var summary = quotaPolicyService.summarizeUsage(actor, days);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("days", summary.days());
        out.put("todayTokens", summary.todayTokens());
        out.put("globalDayTokens", summary.globalDayTokens());
        out.put("periodTokens", summary.periodTokens());
        out.put("teachersUsed", summary.teachersUsed());
        out.put("byDay", summary.byDay());
        out.put("byTeacher",
                McpInput.paginateItems(summary.byTeacher(), in.readPage(), in.readSize()));
        out.put("byModel", summary.byModel());
        return out;
    }
}
