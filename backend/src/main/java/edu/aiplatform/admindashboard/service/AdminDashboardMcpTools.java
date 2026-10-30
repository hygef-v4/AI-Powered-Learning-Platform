package edu.aiplatform.admindashboard.service;
import edu.aiplatform.ai.contract.*;
import edu.aiplatform.identity.contract.*;
import java.util.*;
import org.springframework.stereotype.Component;
@Component
public class AdminDashboardMcpTools implements IMcpToolProvider {
    private final AdminDashboardService dashboard;
    public AdminDashboardMcpTools(AdminDashboardService dashboard){this.dashboard=dashboard;}
    @Override public List<McpToolDefinition> tools(){return List.of(new McpToolDefinition("read_platform_health_summary",
            "Đọc tổng quan tài khoản, môn, lớp và tình trạng backup; dependency lỗi được đánh dấu unavailable.",
            McpSchemas.schema(""),McpToolKind.READ,Set.of(Role.ADMIN)));}
    @Override public McpToolResult call(Actor actor,String name,Map<String,Object> args){
        McpInput.admin(actor);if(!"read_platform_health_summary".equals(name))throw McpInput.invalid();
        return new McpToolResult(false,dashboard.dashboard(actor));
    }
}
