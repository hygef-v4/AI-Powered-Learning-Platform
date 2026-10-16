package edu.aiplatform.files.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import edu.aiplatform.files.application.StoredMetadata;
import edu.aiplatform.files.contract.ArtifactPurpose;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalFolderStorageAdapterTest {

    @TempDir
    Path dir;

    @Test
    void storesReadsAndDeletesWithMetadata() throws Exception {
        LocalFolderStorageAdapter storage = new LocalFolderStorageAdapter(dir);
        Path content = Files.writeString(dir.resolve("in.txt"), "noi dung");
        StoredMetadata metadata = new StoredMetadata(UUID.randomUUID(), ArtifactPurpose.MATERIAL, "a.pdf",
                "application/pdf", 8, "sha");

        String fileId = storage.put(ArtifactPurpose.MATERIAL, content, metadata);

        assertThat(storage.metadata(fileId)).contains(metadata);
        try (var in = storage.open(fileId)) {
            assertThat(new String(in.readAllBytes())).isEqualTo("noi dung");
        }
        storage.delete(fileId);
        storage.delete(fileId);
        assertThat(storage.metadata(fileId)).isEmpty();
    }

    @Test
    void ignoresIdsThatCouldEscapeTheFolder() {
        LocalFolderStorageAdapter storage = new LocalFolderStorageAdapter(dir);

        assertThat(storage.metadata("../../etc/passwd")).isEmpty();
    }
}
