package edu.aiplatform.backup.service;

import edu.aiplatform.backup.repository.BackupRunRepository;
import edu.aiplatform.jobs.contract.IPendingSweeper;
import edu.aiplatform.jobs.contract.JobTypes;
import edu.aiplatform.jobs.contract.PendingJob;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Gửi lại {@code BACKUP_RUN} cho lần chạy kẹt ở {@code CREATED} quá 5 phút (BR-U03-58). */
@Component
public class BackupPendingSweeper implements IPendingSweeper {

    private final BackupRunRepository runs;

    public BackupPendingSweeper(BackupRunRepository runs) {
        this.runs = runs;
    }

    @Override
    public String jobType() {
        return JobTypes.BACKUP_RUN;
    }

    @Override
    public List<PendingJob> findStalePending(Instant olderThan) {
        return runs.findCreatedBefore(olderThan).stream()
                .map(run -> new PendingJob("backup:" + run.scheduledFor(), Map.of("runId", run.id().toString())))
                .toList();
    }
}
