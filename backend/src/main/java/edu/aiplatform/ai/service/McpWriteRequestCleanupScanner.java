package edu.aiplatform.ai.service;

import edu.aiplatform.jobs.contract.IScheduledScanner;
import java.time.Instant;
import org.springframework.stereotype.Component;

/** Mỗi phút xóa bản ghi chống ghi trùng MCP cũ hơn 7 ngày, để bảng không tăng mãi. */
@Component
public class McpWriteRequestCleanupScanner implements IScheduledScanner {

    private final McpWriteRequestService mcpWriteRequestService;

    public McpWriteRequestCleanupScanner(McpWriteRequestService mcpWriteRequestService) {
        this.mcpWriteRequestService = mcpWriteRequestService;
    }

    @Override
    public String name() {
        return "mcp-write-requests";
    }

    @Override
    public void scan(Instant now) {
        mcpWriteRequestService.deleteExpiredRequests(now);
    }
}
