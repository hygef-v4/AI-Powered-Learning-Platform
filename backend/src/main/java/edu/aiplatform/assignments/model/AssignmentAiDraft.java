package edu.aiplatform.assignments.model;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

/**
 * Đề xuất của AI hoặc agent MCP cho bài nháp (P5): không sửa thẳng bài; chỉ lệnh Áp dụng kèm {@code version} mới
 * thay khung. {@code proposal} gồm hướng dẫn, các mục heading kèm đoạn hướng dẫn và gợi ý rubric.
 */
public record AssignmentAiDraft(UUID id, UUID assignmentId, Origin origin, UUID taskId, String requestRef,
                                Status status, JsonNode proposal, JsonNode provenance, long baseVersion,
                                String safeError, UUID createdBy, Instant createdAt, Instant decidedAt) {

    public enum Origin { AI_TASK, MCP }

    public enum Status { PENDING, READY, APPLIED, DISCARDED, FAILED }
}
