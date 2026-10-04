package edu.aiplatform.files.port;

import java.io.InputStream;

public record UploadedFile(String originalFileName, String declaredContentType, long byteSize, InputStream content) {}
