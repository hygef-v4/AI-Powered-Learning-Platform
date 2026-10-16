package edu.aiplatform.ai.contract;

import edu.aiplatform.identity.contract.Actor;
import java.util.List;
import java.util.Map;

/**
 * U06 khai báo, unit sở hữu dữ liệu cài (U07, U08, U09, U10, U12, U13): đăng ký tool MCP, tự kiểm quyền
 * và {@code expectedVersion} khi ghi.
 */
public interface IMcpToolProvider {

    List<McpToolDefinition> tools();

    McpToolResult call(Actor actor, String toolName, Map<String, Object> args);
}
