package edu.aiplatform.files.service;

/** Drive từ chối trả tệp vì bị đánh dấu abuse (BR-U03-24). */
public class FileAbusiveException extends RuntimeException {

    public FileAbusiveException(String fileId) {
        super("Drive từ chối trả tệp");
    }
}
