package edu.aiplatform.ai.model;

import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.ai.contract.UsageMeasurement;
import java.time.Instant;
import java.util.UUID;

/**
 * Một dòng usage chỉ thêm (domain-entities U06 mục 4). Không biết số token thì để rỗng với {@code UNAVAILABLE},
 * không ghi 0 (BR-U06-35). Không có prompt, key hay nội dung.
 */
public record AiUsageRecord(UUID id, UUID accountId, Source source, UUID taskId, UUID mcpCredentialId,
                            AiProvider provider, String model, String taskType, String toolName, String outcome,
                            Long inputTokens, Long outputTokens, Long cachedTokens, Long reasoningTokens,
                            UsageMeasurement provenance, Instant createdAt) {

    public enum Source {
        SHARED, PERSONAL, MCP
    }
}
