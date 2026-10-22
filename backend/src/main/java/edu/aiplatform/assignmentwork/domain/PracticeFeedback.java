package edu.aiplatform.assignmentwork.domain;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

/**
 * Phản hồi AI cho lượt Practice đã nộp (BR-U08-40…45): gắn {@code submissionHash} của lượt; nhãn "AI ước tính,
 * không phải điểm chính thức"; không vào sổ điểm.
 */
public record PracticeFeedback(UUID id, UUID attemptId, UUID accountId, Origin origin, UUID taskId, String requestRef,
                               Status status, String submissionHash, JsonNode feedback, JsonNode provenance,
                               String safeError, Instant createdAt, Instant completedAt) {

    public enum Origin { AI_TASK, MCP }

    public enum Status { PENDING, READY, FAILED }
}
