package edu.aiplatform.files.service;

/** Nơi lưu tệp không dùng được (chưa cấu hình, mạng, lỗi tạm của Drive). */
public class StorageUnavailableException extends RuntimeException {

    public StorageUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
