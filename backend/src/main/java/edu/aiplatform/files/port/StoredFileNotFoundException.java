package edu.aiplatform.files.port;

/** Không có tệp với `fileId` này. */
public class StoredFileNotFoundException extends RuntimeException {
    public StoredFileNotFoundException(String message) {
        super(message);
    }
}
