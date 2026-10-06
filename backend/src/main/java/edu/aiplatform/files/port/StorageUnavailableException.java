package edu.aiplatform.files.port;

/** Kho lưu trữ tạm thời không dùng được hoặc cấu hình sai (BR-U03-41, NFR-U03-14). */
public class StorageUnavailableException extends RuntimeException {
    public StorageUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
