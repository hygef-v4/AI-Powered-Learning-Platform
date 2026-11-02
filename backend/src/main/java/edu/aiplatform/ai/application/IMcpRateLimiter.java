package edu.aiplatform.ai.application;

import java.util.UUID;

/** Giới hạn lời gọi tool MCP: tối đa 60 lời gọi mỗi phút mỗi credential (BR-U06-33). */
public interface IMcpRateLimiter {

    boolean tryAcquire(UUID credentialId);
}
