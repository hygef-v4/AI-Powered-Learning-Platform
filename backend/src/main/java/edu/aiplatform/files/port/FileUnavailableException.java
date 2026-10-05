package edu.aiplatform.files.port;

/** Drive từ chối trả tệp vì bị đánh dấu abuse (`cannotDownloadAbusiveFile`, BR-U03-24). */
public class FileUnavailableException extends RuntimeException {
    public FileUnavailableException(String message) {
        super(message);
    }
}
