package edu.aiplatform.ai.contract;

/** Kết quả tool; U06 cắt còn tối đa 256 KB trước khi trả agent. */
public record McpToolResult(boolean error, Object content) {
}
