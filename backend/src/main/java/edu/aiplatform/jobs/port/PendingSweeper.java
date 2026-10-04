package edu.aiplatform.jobs.port;

import java.time.Instant;
import java.util.List;

/** Mỗi phút: dòng còn chờ quá 5 phút được gửi lại (BR-U03-58). */
public interface PendingSweeper {
    String jobType();

    List<JobMessage> findStalePending(Instant olderThan);
}
