package edu.aiplatform.backup.application;

import edu.aiplatform.backup.domain.BackupRunRepository;
import edu.aiplatform.jobs.contract.IJob;
import edu.aiplatform.jobs.contract.IScheduledScanner;
import edu.aiplatform.jobs.contract.JobTypes;
import edu.aiplatform.settings.contract.ISettings;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** B1: mỗi phút, đến giờ trong Settings thì tạo lần chạy của ngày (một lần mỗi ngày) và gửi {@code BACKUP_RUN}. */
@Component
public class BackupScheduleScanner implements IScheduledScanner {

    static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final ISettings settings;
    private final BackupRunRepository runs;
    private final IJob jobs;

    public BackupScheduleScanner(ISettings settings, BackupRunRepository runs, IJob jobs) {
        this.settings = settings;
        this.runs = runs;
        this.jobs = jobs;
    }

    @Override
    public String name() {
        return "backup-schedule";
    }

    @Override
    @Transactional
    public void scan(Instant now) {
        boolean enabled = settings.get(BackupSettingDefinitions.ENABLED).orElse(false);
        LocalTime dailyAt = settings.get(BackupSettingDefinitions.DAILY_AT).orElse(null);
        if (!enabled || dailyAt == null) {
            return;
        }
        ZonedDateTime local = now.atZone(ZONE);
        if (local.toLocalTime().isBefore(dailyAt)) {
            return;
        }
        runs.createIfAbsent(local.toLocalDate(), now).ifPresent(runId ->
                jobs.enqueue(JobTypes.BACKUP_RUN, Map.of("runId", runId.toString()), "backup:" + local.toLocalDate()));
    }
}
