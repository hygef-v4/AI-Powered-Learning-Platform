package edu.aiplatform.backup.integration;

import edu.aiplatform.backup.config.BackupProperties;
import edu.aiplatform.backup.service.BackupFailure;
import edu.aiplatform.backup.service.IBaseBackupRunner;
import edu.aiplatform.jobs.contract.RetryableJobException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

/**
 * {@code pg_basebackup} bằng role {@code backup_replicator} (U03, 2026-11-26): ghi một tệp tar nén gzip ra stdout,
 * kèm các tệp WAL cần để bản chụp tự nhất quán ({@code --wal-method=fetch}). Checkpoint giãn và giới hạn tốc độ đọc để
 * không ảnh hưởng người dùng.
 */
@Component
public class PgBaseBackupRunner implements IBaseBackupRunner {

    static final long TIMEOUT_MINUTES = 100;

    private final BackupProperties properties;

    public PgBaseBackupRunner(BackupProperties properties) {
        this.properties = properties;
    }

    @Override
    public void runBaseBackup(Path target) {
        ProcessBuilder builder = new ProcessBuilder(buildCommand(properties))
                .redirectOutput(target.toFile())
                .redirectError(target.resolveSibling("pg_basebackup.log").toFile());
        builder.environment().put("PGPASSWORD", properties.dbPassword() == null ? "" : properties.dbPassword());
        Process process;
        try {
            process = builder.start();
        } catch (IOException e) {
            throw new BackupFailure("Không chạy được pg_basebackup trong worker");
        }
        try {
            if (!process.waitFor(TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
                process.destroyForcibly();
                throw new RetryableJobException("pg_basebackup quá thời gian");
            }
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new RetryableJobException("pg_basebackup bị ngắt");
        }
        if (process.exitValue() != 0) {
            throw new RetryableJobException("pg_basebackup lỗi, mã thoát " + process.exitValue());
        }
    }

    static List<String> buildCommand(BackupProperties properties) {
        List<String> command = new ArrayList<>(List.of("pg_basebackup", "--pgdata=-", "--format=tar",
                "--wal-method=fetch", "--gzip", "--checkpoint=spread", "--no-password",
                "--host", properties.dbHost(), "--port", String.valueOf(properties.dbPort()),
                "--username", properties.dbUser()));
        if (properties.maxRate() != null && !properties.maxRate().isBlank()) {
            command.add("--max-rate=" + properties.maxRate().strip());
        }
        return command;
    }
}
