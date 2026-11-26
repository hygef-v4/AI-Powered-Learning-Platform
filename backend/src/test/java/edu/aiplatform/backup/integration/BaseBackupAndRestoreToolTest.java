package edu.aiplatform.backup.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.aiplatform.backup.config.BackupProperties;
import edu.aiplatform.backup.service.BackupEncryptor;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BaseBackupAndRestoreToolTest {

    @TempDir
    Path dir;

    static BackupRestoreTool.DriveEntry folder(String name, String createdAt) {
        return new BackupRestoreTool.DriveEntry(name + "-id", name, Instant.parse(createdAt), true);
    }

    static BackupRestoreTool.DriveEntry file(String name, String createdAt) {
        return new BackupRestoreTool.DriveEntry(name + "-id", name, Instant.parse(createdAt), false);
    }

    @Test
    void baseBackupStreamsOneGzipTarWithFetchedWalAndRateLimit() {
        BackupProperties props = new BackupProperties(null, null, "postgres", 5432, "platform", "backup_replicator",
                "", null, null, "32M");

        assertThat(PgBaseBackupRunner.buildCommand(props)).containsExactly("pg_basebackup", "--pgdata=-",
                "--format=tar", "--wal-method=fetch", "--gzip", "--checkpoint=spread", "--no-password",
                "--host", "postgres", "--port", "5432", "--username", "backup_replicator", "--max-rate=32M");
    }

    @Test
    void picksTheNewestBaseBackupNotAfterTheTargetAndIgnoresTheWalFolder() {
        var children = List.of(folder("run-1", "2026-11-24T19:05:00Z"), folder("run-2", "2026-11-25T19:05:00Z"),
                folder("wal", "2026-11-26T00:00:00Z"));

        assertThat(BackupRestoreTool.selectBase(children, null)).map(BackupRestoreTool.DriveEntry::name).contains("run-2");
        assertThat(BackupRestoreTool.selectBase(children, Instant.parse("2026-11-25T10:00:00Z")))
                .map(BackupRestoreTool.DriveEntry::name).contains("run-1");
        assertThat(BackupRestoreTool.selectBase(children, Instant.parse("2026-11-20T00:00:00Z"))).isEmpty();
    }

    @Test
    void takesWalFromOneHourBeforeTheBaseBackupInNameOrder() {
        var wal = List.of(file("000000010000000000000003.gz", "2026-11-25T19:10:00Z"),
                file("000000010000000000000001.gz", "2026-11-25T17:00:00Z"),
                file("000000010000000000000002.gz", "2026-11-25T18:30:00Z"));

        assertThat(BackupRestoreTool.selectWal(wal, Instant.parse("2026-11-25T19:05:00Z")))
                .extracting(BackupRestoreTool.DriveEntry::name)
                .containsExactly("000000010000000000000002.gz", "000000010000000000000003.gz");
    }

    @Test
    void restoresUploadedNamesToOriginalWalNames() {
        assertThat(BackupRestoreTool.walName("000000010000000000000003.gz.alapbk")).isEqualTo("000000010000000000000003");
        assertThat(BackupRestoreTool.walName("000000010000000000000003.gz")).isEqualTo("000000010000000000000003");
        assertThat(BackupRestoreTool.walName("00000002.history.gz")).isEqualTo("00000002.history");
    }

    @Test
    void decryptsAndDecompressesAnEncryptedWalFile() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keys = generator.generateKeyPair();
        Path gz = dir.resolve("wal.gz");
        try (OutputStream out = new GZIPOutputStream(Files.newOutputStream(gz))) {
            out.write("wal bytes".getBytes());
        }
        Path encrypted = dir.resolve("wal.gz.alapbk");
        try (var in = Files.newInputStream(gz); var out = Files.newOutputStream(encrypted)) {
            BackupEncryptor.encryptArchive(in, out, keys.getPublic());
        }

        Path restored = dir.resolve("000000010000000000000003");
        BackupRestoreTool.restoreWalFile(encrypted, restored, keys.getPrivate());

        assertThat(Files.readString(restored)).isEqualTo("wal bytes");
        assertThatThrownBy(() -> BackupRestoreTool.restoreWalFile(encrypted, dir.resolve("x"), null))
                .hasMessageContaining("--private-key");
    }

    @Test
    void parsesTargetTimeAndPrivateKeyOptions() {
        var options = BackupRestoreTool.parseOptions(new String[] {"/restore", "--target-time",
                "2026-11-26T16:55:00+07:00", "--private-key", "/keys/private.pem"});

        assertThat(options.outDir()).isEqualTo(Path.of("/restore"));
        assertThat(options.targetTime()).isEqualTo(Instant.parse("2026-11-26T09:55:00Z"));
        assertThat(options.privateKey()).isEqualTo(Path.of("/keys/private.pem"));
        assertThatThrownBy(() -> BackupRestoreTool.parseOptions(new String[] {"/restore", "--bad", "x"}))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
