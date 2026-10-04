package edu.aiplatform.jobs.port;

import java.time.Instant;

/** Việc theo thời gian, chạy mỗi phút trong worker; cập nhật có điều kiện (BR-U03-60). */
public interface ScheduledScanner {
    String name();

    void scan(Instant now);
}
