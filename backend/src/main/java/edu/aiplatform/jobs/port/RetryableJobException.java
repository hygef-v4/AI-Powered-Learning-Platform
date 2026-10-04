package edu.aiplatform.jobs.port;

/** Lỗi tạm: U03 thử lại theo backoff 30 s, 1, 2, 4, 8 phút (BR-U03-56). */
public class RetryableJobException extends RuntimeException {
    public RetryableJobException(String message, Throwable cause) { super(message, cause); }
}
