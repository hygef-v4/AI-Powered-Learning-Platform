package edu.aiplatform.jobs.contract;

import java.time.Instant;

/** U03 khai báo: việc hẹn giờ chạy mỗi phút trong worker, cập nhật có điều kiện để chạy lại không trùng. */
public interface IScheduledScanner {

    String name();

    void scan(Instant now);
}
