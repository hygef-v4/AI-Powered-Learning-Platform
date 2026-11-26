package edu.aiplatform.ai.contract;

import edu.aiplatform.identity.contract.Actor;
import java.util.List;
import java.util.Map;

/**
 * U06 khai báo, unit sở hữu dữ liệu cài (U07, U08, U09, U10, U12, U13): đăng ký tool MCP, tự kiểm quyền
 * và {@code expectedVersion} khi ghi.
 */
public interface IMcpToolProvider {

    // Cung cấp schema, loại và vai trò của tool để đăng ký trước khi dispatch.
    List<McpToolDefinition> tools();

    // Nhận danh tính, tên tool và tham số đã qua dispatch, rồi trả kết quả của nghiệp vụ.
    McpToolResult call(Actor actor, String toolName, Map<String, Object> args);
}
