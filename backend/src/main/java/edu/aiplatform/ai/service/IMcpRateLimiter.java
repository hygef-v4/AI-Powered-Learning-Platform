package edu.aiplatform.ai.service;

import java.util.UUID;

/** Giới hạn lời gọi tool MCP: tối đa 60 lời gọi mỗi phút mỗi credential (BR-U06-33). */
public interface IMcpRateLimiter {

    // Yêu cầu một lượt gọi cho token và trả false nếu đã dùng hết hạn mức.
    boolean tryAcquire(UUID credentialId);
}
