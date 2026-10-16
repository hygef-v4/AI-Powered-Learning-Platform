package edu.aiplatform.jobs.contract;

import java.time.Duration;

/**
 * U03 khai báo, unit sở hữu loại việc cài: xử lý một loại việc trong worker. {@code handle} idempotent;
 * {@code onFailed} chuyển dòng nghiệp vụ sang lỗi khi hết lượt thử.
 */
public interface IJobHandler {

    String jobType();

    JobQueue queue();

    void handle(JobMessage message);

    void onFailed(JobMessage message, String error);

    Duration timeout();
}
