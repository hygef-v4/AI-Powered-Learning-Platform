package edu.aiplatform.jobs.port;

/** Handler của unit sở hữu; phải idempotent (BR-U03-51, 59). */
public interface JobHandler {
    String jobType();

    JobQueue queue();

    /** Ném {@link RetryableJobException} khi lỗi tạm. */
    void handle(JobMessage message);

    /** Lỗi vĩnh viễn hoặc hết lượt: chuyển dòng nghiệp vụ sang trạng thái lỗi. */
    void onFailed(JobMessage message, Throwable lastError);
}
