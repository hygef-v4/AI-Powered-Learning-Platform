package edu.aiplatform.content.port;

import java.math.BigDecimal;
import java.util.UUID;

/** U05 khai báo, U13 cài (`C`): kill-switch, trần, tần suất, giữ credit, ghi `ai_suggestions`. */
public interface AiUsagePort {
    AiUsageTicket begin(String taskType, UUID requestedBy, String targetType, UUID targetId, String requestRef);
    void complete(UUID suggestionId, long inputTokens, long outputTokens, BigDecimal costUsd);
    void fail(UUID suggestionId);
}
