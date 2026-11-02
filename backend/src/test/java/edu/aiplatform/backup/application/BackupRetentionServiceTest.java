package edu.aiplatform.backup.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.aiplatform.backup.contract.BackupState;
import edu.aiplatform.backup.domain.BackupRun;
import edu.aiplatform.backup.domain.BackupRunRepository;
import edu.aiplatform.files.application.StorageUnavailableException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BackupRetentionServiceTest {

    final BackupRunRepository runs = mock(BackupRunRepository.class);
    final IBackupStore store = mock(IBackupStore.class);
    final BackupRetentionService retention = new BackupRetentionService(runs, store);
    final Instant now = Instant.parse("2026-10-16T19:30:00Z");

    static BackupRun success(Instant snapshotAt) {
        return new BackupRun(UUID.randomUUID(), LocalDate.of(2026, 10, 1), BackupState.SUCCESS, snapshotAt, snapshotAt,
                "1", "dev", "ref", "sha", 10L, snapshotAt, snapshotAt, null, null);
    }

    @Test
    void asksOnlyForSuccessfulRunsOlderThanSevenDays() {
        retention.purgeOld(now);

        verify(runs).findPurgeCandidates(Instant.parse("2026-10-09T19:30:00Z"));
    }

    @Test
    void deletesOldArchivesAndMarksThemPurged() {
        BackupRun old = success(Instant.parse("2026-10-01T19:00:00Z"));
        when(runs.findPurgeCandidates(any())).thenReturn(List.of(old));

        retention.purgeOld(now);

        verify(store).delete(old.id().toString());
        verify(runs).markPurged(old.id(), now);
    }

    @Test
    void keepsTheRowWhenDriveCannotDeleteNow() {
        BackupRun old = success(Instant.parse("2026-10-01T19:00:00Z"));
        when(runs.findPurgeCandidates(any())).thenReturn(List.of(old));
        doThrow(new StorageUnavailableException("down", null)).when(store).delete(any());

        retention.purgeOld(now);

        verify(runs, never()).markPurged(any(), any());
    }
}
