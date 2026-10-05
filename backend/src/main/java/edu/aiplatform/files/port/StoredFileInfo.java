package edu.aiplatform.files.port;

public record StoredFileInfo(String fileId, String mediaType, long byteSize, String originalFileName) {}
