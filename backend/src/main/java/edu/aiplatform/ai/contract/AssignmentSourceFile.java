package edu.aiplatform.ai.contract;

/** Metadata đã được U03 xác minh sau khi gắn FileRef của nguồn riêng cho bài tập. */
public record AssignmentSourceFile(String fileId, String fileName, String mediaType, long byteSize) {
}
