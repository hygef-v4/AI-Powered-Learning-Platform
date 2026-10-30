package edu.aiplatform.backup.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.aiplatform.backup.repository.BackupRunRepository;
import edu.aiplatform.jobs.contract.IJob;
import edu.aiplatform.jobs.contract.JobTypes;
import edu.aiplatform.settings.contract.ISettings;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BackupScheduleScannerTest {

    final ISettings settings = mock(ISettings.class);
    final BackupRunRepository runs = mock(BackupRunRepository.class);
    final IJob jobs = mock(IJob.class);
    final BackupScheduleScanner scanner = new BackupScheduleScanner(settings, runs, jobs);

    void configure(boolean enabled, LocalTime dailyAt) {
        when(settings.get(BackupSettingDefinitions.ENABLED)).thenReturn(Optional.of(enabled));
        when(settings.get(BackupSettingDefinitions.DAILY_AT)).thenReturn(Optional.ofNullable(dailyAt));
    }

    @Test
    void doesNothingWhenDisabledOrTimeNotSet() {
        configure(false, LocalTime.of(2, 0));
        scanner.scan(Instant.parse("2026-10-16T10:00:00Z"));
        configure(true, null);
        scanner.scan(Instant.parse("2026-10-16T10:00:00Z"));

        verify(runs, never()).createIfAbsent(any(), any());
    }

    @Test
    void waitsUntilTheConfiguredTimeInVietnam() {
        configure(true, LocalTime.of(2, 0));

        scanner.scan(Instant.parse("2026-10-16T18:59:00Z"));

        verify(runs, never()).createIfAbsent(any(), any());
    }

    @Test
    void createsOneRunForTheVietnamDateAndEnqueuesIt() {
        configure(true, LocalTime.of(2, 0));
        UUID runId = UUID.randomUUID();
        Instant now = Instant.parse("2026-10-16T19:00:00Z");
        when(runs.createIfAbsent(LocalDate.of(2026, 10, 17), now)).thenReturn(Optional.of(runId));

        scanner.scan(now);

        verify(jobs).enqueue(JobTypes.BACKUP_RUN, Map.of("runId", runId.toString()), "backup:2026-10-17");
    }

    @Test
    void secondScanOfTheSameDayDoesNotEnqueueAgain() {
        configure(true, LocalTime.of(2, 0));
        when(runs.createIfAbsent(any(), any())).thenReturn(Optional.empty());

        scanner.scan(Instant.parse("2026-10-16T20:00:00Z"));

        verify(jobs, never()).enqueue(anyString(), anyMap(), eq("backup:2026-10-17"));
    }
}
