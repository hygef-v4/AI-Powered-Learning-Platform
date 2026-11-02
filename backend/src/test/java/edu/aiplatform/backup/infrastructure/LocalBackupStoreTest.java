package edu.aiplatform.backup.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import edu.aiplatform.backup.application.IBackupStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalBackupStoreTest {

    @TempDir
    Path dir;

    @Test
    void storesWithChecksumAndDeletesTheRunFolder() throws Exception {
        LocalBackupStore store = new LocalBackupStore(dir.resolve("backups"));
        UUID runId = UUID.randomUUID();
        Path archive = Files.writeString(dir.resolve("a.bin"), "abc");

        IBackupStore.StoredArchive stored = store.put(runId, archive);

        assertThat(stored.byteSize()).isEqualTo(3);
        assertThat(stored.md5()).isEqualTo("900150983cd24fb0d6963f7d28e17f72");
        store.delete(runId.toString());
        store.delete(runId.toString());
        assertThat(dir.resolve("backups").resolve(runId.toString())).doesNotExist();
    }
}
