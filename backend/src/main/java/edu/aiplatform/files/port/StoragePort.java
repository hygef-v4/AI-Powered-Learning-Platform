package edu.aiplatform.files.port;

import java.io.InputStream;
import java.util.Map;

/** Google Drive hoặc thư mục local; metadata nằm trong `appProperties`, không có bảng (P5). */
public interface StoragePort {
    String put(InputStream content, Map<String, String> metadata);
    InputStream get(String fileId);
    Map<String, String> readMetadata(String fileId);
    void delete(String fileId);
}
