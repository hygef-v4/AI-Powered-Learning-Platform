package edu.aiplatform.backup.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.backup.domain.BackupRunRepository;
import edu.aiplatform.backup.infrastructure.LocalBackupStore;
import edu.aiplatform.jobs.contract.DomainEvent;
import edu.aiplatform.jobs.contract.EventTypes;
import edu.aiplatform.jobs.contract.IEventPublisher;
import edu.aiplatform.jobs.contract.IJob;
import edu.aiplatform.jobs.contract.JobMessage;
import edu.aiplatform.jobs.contract.JobTypes;
import edu.aiplatform.jobs.contract.RetryableJobException;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

class BackupJobHandlerTest {

    static KeyPair keys;

    @TempDir
    Path dir;

    final BackupRunRepository runs = mock(BackupRunRepository.class);
    final BackupRetentionService retention = mock(BackupRetentionService.class);
    final IAudit audit = mock(IAudit.class);
    final IEventPublisher events = mock(IEventPublisher.class);
    final IJob jobs = mock(IJob.class);
    final Instant now = Instant.parse("2026-10-16T19:00:00Z");
    final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    final UUID runId = UUID.randomUUID();
    final JobMessage message = new JobMessage(1, JobTypes.BACKUP_RUN, "backup:2026-10-17",
            Map.of("runId", runId.toString()), 0, "c");

    @BeforeAll
    static void generateKeys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keys = generator.generateKeyPair();
    }

    BackupJobHandler handler(String publicKey, IBackupStore store) {
        BackupProperties props = new BackupProperties(null, publicKey, "localhost", 5432, "platform", "backup_reader",
                "", dir.resolve("tmp").toString(), null);
        IDumpRunner dump = target -> {
            try {
                Files.writeString(target, "-- dump của database");
            } catch (java.io.IOException e) {
                throw new IllegalStateException(e);
            }
        };
        return new BackupJobHandler(runs, dump, store, retention, audit, events, jobs, props, clock);
    }

    String publicKey() {
        return Base64.getEncoder().encodeToString(keys.getPublic().getEncoded());
    }

    @Test
    void finishedRunIsSkipped() {
        when(runs.markRunning(runId, now)).thenReturn(0);

        handler(publicKey(), mock(IBackupStore.class)).handle(message);

        verify(runs, never()).markSuccess(any(), any(), any(), any(), any(), any(), anyLong(), any());
    }

    @Test
    void successfulRunStoresAnEncryptedArchiveThatDecryptsToTheDump() throws Exception {
        when(runs.markRunning(runId, now)).thenReturn(1);
        when(runs.schemaVersion()).thenReturn("20260925.0920");
        LocalBackupStore store = new LocalBackupStore(dir.resolve("backups"));

        handler(publicKey(), store).handle(message);

        verify(runs).markSuccess(eq(runId), eq(now), eq("20260925.0920"), anyString(), eq(runId.toString()),
                anyString(), anyLong(), eq(now));
        verify(retention).purgeOld(now);
        ArgumentCaptor<AuditEvent> event = ArgumentCaptor.forClass(AuditEvent.class);
        verify(audit).record(event.capture());
        assertThat(event.getValue().action()).isEqualTo("BACKUP_COMPLETED");
        Path archive = dir.resolve("backups").resolve(runId.toString()).resolve("archive.alapbk");
        ByteArrayOutputStream plain = new ByteArrayOutputStream();
        try (var in = Files.newInputStream(archive)) {
            BackupEncryptor.decrypt(in, plain, keys.getPrivate());
        }
        assertThat(plain.toString()).isEqualTo("-- dump của database");
        assertThat(dir.resolve("tmp").resolve(runId.toString())).doesNotExist();
    }

    @Test
    void sizeMismatchAfterUploadIsRetried() {
        when(runs.markRunning(runId, now)).thenReturn(1);
        IBackupStore store = mock(IBackupStore.class);
        when(store.put(any(), any())).thenReturn(new IBackupStore.StoredArchive("ref", 1, null));

        assertThatThrownBy(() -> handler(publicKey(), store).handle(message)).isInstanceOf(RetryableJobException.class);
        verify(store).delete(runId.toString());
    }

    @Test
    void missingPublicKeyFailsWithoutRetry() {
        when(runs.markRunning(runId, now)).thenReturn(1);

        assertThatThrownBy(() -> handler("", mock(IBackupStore.class)).handle(message))
                .isInstanceOf(BackupFailure.class);
    }

    @Test
    void failureMarksTheRunCleansUpAuditsAndNotifies() {
        when(runs.markFailed(eq(runId), anyString(), eq(now))).thenReturn(1);
        when(runs.find(runId)).thenReturn(Optional.empty());

        handler(publicKey(), mock(IBackupStore.class)).onFailed(message, "Drive trả lỗi 403");

        verify(jobs).enqueue(eq(JobTypes.DRIVE_CLEANUP), eq(Map.of("storage", "BACKUP", "ref", runId.toString())),
                anyString());
        ArgumentCaptor<DomainEvent> event = ArgumentCaptor.forClass(DomainEvent.class);
        verify(events).publish(event.capture());
        assertThat(event.getValue().eventType()).isEqualTo(EventTypes.BACKUP_FAILED);
        ArgumentCaptor<AuditEvent> audited = ArgumentCaptor.forClass(AuditEvent.class);
        verify(audit).record(audited.capture());
        assertThat(audited.getValue().action()).isEqualTo("BACKUP_FAILED");
    }

    @Test
    void successfulRunIsNeverTurnedIntoFailure() {
        when(runs.markFailed(eq(runId), anyString(), eq(now))).thenReturn(0);

        handler(publicKey(), mock(IBackupStore.class)).onFailed(message, "lỗi muộn");

        verify(jobs, never()).enqueue(anyString(), anyMap(), anyString());
        verify(events, never()).publish(any());
    }
}
