package edu.aiplatform.files.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import edu.aiplatform.files.domain.StoredFile;
import edu.aiplatform.files.port.ArtifactPurpose;
import edu.aiplatform.files.port.StoragePort;
import edu.aiplatform.files.port.StorageUnavailableException;
import edu.aiplatform.files.port.StoredFileNotFoundException;
import edu.aiplatform.files.worker.DriveJobHandler;
import edu.aiplatform.jobs.port.JobMessage;
import edu.aiplatform.jobs.port.JobTypes;
import edu.aiplatform.jobs.port.RetryableJobException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** P5 (adapter local), Bước 9 (`DRIVE_CLEANUP`), Bước 13 (metadata đủ trường), Bước 14 (FileRef). */
class StorageAndCleanupTest {

    @TempDir
    Path dir;

    @Test
    void localAdapterRoundTripsBytesAndMetadata() throws Exception {
        LocalFolderStorageAdapter local = new LocalFolderStorageAdapter(dir.resolve("files"));
        UUID owner = UUID.randomUUID();
        Path tmp = Files.write(dir.resolve("tmp"), new byte[] {9, 8, 7, 6});

        String id = local.put(tmp, StoredFile.metadata(owner, ArtifactPurpose.DOCUMENT_IMAGE, "image/png", "hình.png",
                "f".repeat(64)));

        try (InputStream in = local.get(id)) {
            assertThat(in.readAllBytes()).containsExactly(9, 8, 7, 6);
        }
        StoredFile stored = StoredFile.fromMetadata(id, local.readMetadata(id));
        assertThat(stored.ownerAccountId()).isEqualTo(owner);
        assertThat(stored.purpose()).isEqualTo(ArtifactPurpose.DOCUMENT_IMAGE);
        assertThat(stored.originalFileName()).isEqualTo("hình.png");
        assertThat(stored.byteSize()).isEqualTo(4);

        local.delete(id);
        local.delete(id);
        assertThatThrownBy(() -> local.get(id)).isInstanceOf(StoredFileNotFoundException.class);
    }

    @Test
    void localAdapterRejectsPathTraversalIds() {
        LocalFolderStorageAdapter local = new LocalFolderStorageAdapter(dir);
        assertThatThrownBy(() -> local.get("../../etc/passwd")).isInstanceOf(StoredFileNotFoundException.class);
        assertThatThrownBy(() -> local.readMetadata("..")).isInstanceOf(StoredFileNotFoundException.class);
    }

    @Test
    void missingDriveInProductionMakesStorageUnavailable() {
        UnconfiguredStorageAdapter unconfigured = new UnconfiguredStorageAdapter();
        assertThatThrownBy(() -> unconfigured.put(dir, Map.of())).isInstanceOf(StorageUnavailableException.class);
        assertThatThrownBy(() -> unconfigured.get("x")).isInstanceOf(StorageUnavailableException.class);
    }

    @Test
    void driveCleanupDeletesAndRetriesOnlyTransientErrors() {
        StoragePort storage = mock(StoragePort.class);
        DriveJobHandler handler = new DriveJobHandler(storage);
        JobMessage message = new JobMessage(1, JobTypes.DRIVE_CLEANUP, "k", Map.of("fileId", "abc"), 0, "c");

        assertThatCode(() -> handler.handle(message)).doesNotThrowAnyException();

        doThrow(new StorageUnavailableException("down", null)).when(storage).delete("abc");
        assertThatThrownBy(() -> handler.handle(message)).isInstanceOf(RetryableJobException.class);

        JobMessage noId = new JobMessage(1, JobTypes.DRIVE_CLEANUP, "k", Map.of(), 0, "c");
        assertThatThrownBy(() -> handler.handle(noId)).isInstanceOf(IllegalArgumentException.class);
    }
}
