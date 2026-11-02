package edu.aiplatform.backup.integration;

import edu.aiplatform.backup.service.BackupFailure;
import edu.aiplatform.backup.config.BackupProperties;
import edu.aiplatform.backup.service.IDumpRunner;
import edu.aiplatform.jobs.contract.RetryableJobException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

/** {@code pg_dump --format=custom} bằng role chỉ đọc {@code backup_reader} (P16, NFR-U03-76). */
@Component
public class PgDumpRunner implements IDumpRunner {

    static final long TIMEOUT_MINUTES = 100;

    private final BackupProperties properties;

    public PgDumpRunner(BackupProperties properties) {
        this.properties = properties;
    }

    @Override
    public void dump(Path target) {
        List<String> command = List.of("pg_dump", "--format=custom", "--no-owner", "--no-privileges",
                "--file", target.toString(), "--host", properties.dbHost(), "--port", String.valueOf(properties.dbPort()),
                "--username", properties.dbUser(), "--dbname", properties.dbName());
        ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true)
                .redirectOutput(target.resolveSibling("pg_dump.log").toFile());
        builder.environment().put("PGPASSWORD", properties.dbPassword() == null ? "" : properties.dbPassword());
        Process process;
        try {
            process = builder.start();
        } catch (IOException e) {
            throw new BackupFailure("Không chạy được pg_dump trong worker");
        }
        try {
            if (!process.waitFor(TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
                process.destroyForcibly();
                throw new RetryableJobException("pg_dump quá thời gian");
            }
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new RetryableJobException("pg_dump bị ngắt");
        }
        if (process.exitValue() != 0) {
            throw new RetryableJobException("pg_dump lỗi, mã thoát " + process.exitValue());
        }
    }
}
