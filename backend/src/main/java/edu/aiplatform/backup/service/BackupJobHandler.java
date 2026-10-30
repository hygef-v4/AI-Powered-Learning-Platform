package edu.aiplatform.backup.service;

import edu.aiplatform.backup.config.BackupProperties;

import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.backup.model.BackupRun;
import edu.aiplatform.backup.repository.BackupRunRepository;
import edu.aiplatform.files.service.StorageUnavailableException;
import edu.aiplatform.jobs.contract.DomainEvent;
import edu.aiplatform.jobs.contract.EventTypes;
import edu.aiplatform.jobs.contract.IEventPublisher;
import edu.aiplatform.jobs.contract.IJob;
import edu.aiplatform.jobs.contract.IJobHandler;
import edu.aiplatform.jobs.contract.JobMessage;
import edu.aiplatform.jobs.contract.JobQueue;
import edu.aiplatform.jobs.contract.JobTypes;
import edu.aiplatform.jobs.contract.RetryableJobException;
import edu.aiplatform.jobs.contract.SafeJobErrors;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * {@code BACKUP_RUN} (B2, B4): dump, mã hóa, tải lên thư mục backup, đối chiếu kích thước và MD5, ghi {@code SUCCESS}
 * rồi dọn bản cũ. Lỗi tạm thì thử lại theo việc nền; lỗi vĩnh viễn hoặc hết lượt thì {@code FAILED}, dọn phần dở,
 * audit và phát {@code backup.failed}.
 */
@Component
public class BackupJobHandler implements IJobHandler {

    private static final Logger log = LoggerFactory.getLogger(BackupJobHandler.class);
    static final Duration TIMEOUT = Duration.ofHours(2);

    private final BackupRunRepository runs;
    private final IDumpRunner dumpRunner;
    private final IBackupStore store;
    private final BackupRetentionService retention;
    private final IAudit audit;
    private final IEventPublisher events;
    private final IJob jobs;
    private final BackupProperties properties;
    private final Clock clock;

    public BackupJobHandler(BackupRunRepository runs, IDumpRunner dumpRunner, IBackupStore store,
                            BackupRetentionService retention, IAudit audit, IEventPublisher events, IJob jobs,
                            BackupProperties properties, Clock clock) {
        this.runs = runs;
        this.dumpRunner = dumpRunner;
        this.store = store;
        this.retention = retention;
        this.audit = audit;
        this.events = events;
        this.jobs = jobs;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public String jobType() {
        return JobTypes.BACKUP_RUN;
    }

    @Override
    public JobQueue queue() {
        return JobQueue.BACKUP;
    }

    @Override
    public Duration timeout() {
        return TIMEOUT;
    }

    @Override
    @Transactional
    public void handle(JobMessage message) {
        UUID runId = runId(message);
        // Hold the conditional UPDATE's row lock until completion/rollback. Duplicate workers then see terminal
        // state; a rolled-back attempt can still be claimed by the next retry.
        if (runs.markRunning(runId, clock.instant()) == 0) {
            return;
        }
        PublicKey publicKey = publicKey();
        Path dir = Path.of(tmpDir(), runId.toString());
        try {
            Files.createDirectories(dir);
            ensureSpace(dir);
            Instant snapshotAt = clock.instant();
            Path dump = dir.resolve("dump.pgc");
            dumpRunner.dump(dump);
            Path archive = dir.resolve("archive.alapbk");
            String sha256 = encrypt(dump, archive, publicKey);
            long size = Files.size(archive);
            IBackupStore.StoredArchive stored = store.put(runId, archive);
            if (stored.byteSize() != size || (stored.md5() != null && !stored.md5().equalsIgnoreCase(md5(archive)))) {
                store.delete(runId.toString());
                throw new RetryableJobException("Bản tải lên không khớp kích thước hoặc checksum");
            }
            Instant now = clock.instant();
            if (runs.markSuccess(runId, snapshotAt, runs.schemaVersion(), appVersion(), stored.ref(), sha256, size,
                    now) == 0) {
                throw new RetryableJobException("Chưa ghi được kết quả backup");
            }
            audit.record(new AuditEvent(UUID.randomUUID(), null, "BACKUP_COMPLETED", "BACKUP_RUN", runId.toString(),
                    AuditResult.SUCCESS, null, Map.of("byteSize", size), now));
            purgeAfterCommit(now);
        } catch (StorageUnavailableException | IOException e) {
            throw new RetryableJobException("Lưu trữ hoặc tệp tạm tạm thời lỗi", e);
        } finally {
            deleteRecursively(dir);
        }
    }

    @Override
    @Transactional
    public void onFailed(JobMessage message, String error) {
        UUID runId = runId(message);
        Instant now = clock.instant();
        String safeError = SafeJobErrors.sanitize(error);
        if (runs.markFailed(runId, safeError, now) == 0) {
            return;
        }
        jobs.enqueue(JobTypes.DRIVE_CLEANUP, Map.of("storage", BackupCleanupTarget.AREA, "ref", runId.toString()),
                "cleanup:backup:" + runId);
        audit.record(new AuditEvent(UUID.randomUUID(), null, "BACKUP_FAILED", "BACKUP_RUN", runId.toString(),
                AuditResult.FAILURE, safeError, Map.of(), now));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("runId", runId.toString());
        payload.put("scheduledFor", runs.find(runId).map(BackupRun::scheduledFor).map(Object::toString).orElse(null));
        payload.put("error", safeError);
        events.publish(new DomainEvent(1, UUID.randomUUID(), EventTypes.BACKUP_FAILED, "U03", now, null, payload));
    }

    private void purgeAfterCommit(Instant now) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            retention.purgeOld(now);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    retention.purgeOld(now);
                } catch (RuntimeException e) {
                    // Backup completion is already durable. Retention can retry on the next successful backup.
                    log.warn("Chưa dọn được bản backup cũ; thử lại ở lần backup tiếp theo");
                }
            }
        });
    }

    private void ensureSpace(Path dir) throws IOException {
        long free = Files.getFileStore(dir).getUsableSpace();
        long needed = 2 * runs.databaseSize();
        if (free < needed) {
            throw new BackupFailure("Không đủ dung lượng tạm để dump database");
        }
    }

    private PublicKey publicKey() {
        if (properties.publicKey() == null || properties.publicKey().isBlank()) {
            throw new BackupFailure("Chưa cấu hình U03_BACKUP_PUBLIC_KEY");
        }
        try {
            return BackupEncryptor.parsePublicKey(properties.publicKey());
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new BackupFailure("U03_BACKUP_PUBLIC_KEY không hợp lệ");
        }
    }

    /** Mã hóa dump thành archive, trả SHA-256 của archive. */
    static String encrypt(Path dump, Path archive, PublicKey publicKey) throws IOException {
        try (InputStream in = Files.newInputStream(dump); OutputStream out = Files.newOutputStream(archive)) {
            BackupEncryptor.encrypt(in, out, publicKey);
        }
        return digest(archive, "SHA-256");
    }

    static String md5(Path file) throws IOException {
        return digest(file, "MD5");
    }

    static String digest(Path file, String algorithm) throws IOException {
        try (InputStream in = new DigestInputStream(Files.newInputStream(file), MessageDigest.getInstance(algorithm))) {
            in.transferTo(OutputStream.nullOutputStream());
            return HexFormat.of().formatHex(((DigestInputStream) in).getMessageDigest().digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private String tmpDir() {
        return properties.tmpDir() == null || properties.tmpDir().isBlank()
                ? Path.of(System.getProperty("java.io.tmpdir"), "platform-backup").toString() : properties.tmpDir();
    }

    static String appVersion() {
        String version = BackupJobHandler.class.getPackage().getImplementationVersion();
        return version == null ? "dev" : version;
    }

    static UUID runId(JobMessage message) {
        return UUID.fromString(String.valueOf(message.payload().get("runId")));
    }

    private static void deleteRecursively(Path dir) {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
        } catch (IOException e) {
            // Thư mục tạm sẽ được ghi đè ở lần chạy sau.
        }
    }
}
