package edu.aiplatform.jobs.contract;

import java.time.Instant;
import java.util.List;

/** U03 khai báo: trả các dòng còn chờ quá 5 phút để U03 gửi lại message bị mất. */
public interface IPendingSweeper {

    String jobType();

    List<PendingJob> findStalePending(Instant olderThan);
}
