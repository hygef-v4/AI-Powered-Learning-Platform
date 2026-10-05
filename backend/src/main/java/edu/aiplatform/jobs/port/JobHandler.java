package edu.aiplatform.jobs.port;

import java.time.Duration;
import java.util.Optional;

/**
 * Handler của unit sở hữu; phải idempotent (BR-U03-51, 59). Handler là bean Spring thì tự đăng ký với
 * {@link JobHandlerRegistry}. `queue()` phải khớp bảng định tuyến của U03 (BR-U03-63).
 */
public interface JobHandler {
    String jobType();

    JobQueue queue();

    /** Ném {@link RetryableJobException} khi lỗi tạm; lỗi khác coi là vĩnh viễn. */
    void handle(JobMessage message);

    /** Lỗi vĩnh viễn hoặc hết lượt: chuyển dòng nghiệp vụ sang trạng thái lỗi. */
    void onFailed(JobMessage message, Throwable lastError);

    /** Timeout xử lý riêng của loại việc; rỗng thì dùng `U03_JOB_HANDLER_TIMEOUT` (NFR-U03-46). */
    default Optional<Duration> timeout() {
        return Optional.empty();
    }
}
