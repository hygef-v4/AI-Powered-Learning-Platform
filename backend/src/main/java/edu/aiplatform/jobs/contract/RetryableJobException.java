package edu.aiplatform.jobs.contract;

/** Handler ném khi gặp lỗi tạm (mạng, dịch vụ ngoài 429, 5xx): U03 gửi lại theo backoff (BR-U03-56). */
public class RetryableJobException extends RuntimeException {

    public RetryableJobException(String message, Throwable cause) {
        super(message, cause);
    }

    public RetryableJobException(String message) {
        super(message);
    }
}
