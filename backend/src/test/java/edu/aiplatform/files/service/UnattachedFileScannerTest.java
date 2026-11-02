package edu.aiplatform.files.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.files.config.FilesProperties;
import edu.aiplatform.files.contract.ArtifactPurpose;
import edu.aiplatform.files.integration.UnattachedFiles;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Tệp tải lên mà không ai gắn (hủy, xem trước DOCX rồi bỏ) được dọn; tệp đã gắn thì không. */
class UnattachedFileScannerTest {

    final Instant now = Instant.parse("2026-10-16T08:00:00Z");
    final UnattachedFiles unattached = mock(UnattachedFiles.class);
    final IStorage storage = mock(IStorage.class);

    @Test
    void deletesDueFilesAndForgetsThem() {
        when(unattached.due(now, UnattachedFileScanner.BATCH)).thenReturn(List.of("f1", "f2"));
        doThrow(new StorageUnavailableException("drive down", null)).when(storage).delete("f2");

        new UnattachedFileScanner(unattached, storage).scan(now);

        verify(storage).delete("f1");
        verify(unattached).forget("f1");
        verify(unattached, never()).forget("f2");
    }

    @Test
    void attachingAFileRemovesItFromTheCleanupList() {
        FileRefSigner signer = new FileRefSigner(
                new FilesProperties("secret-of-at-least-32-bytes-long!!", false, null, null, null, null),
                new ObjectMapper());
        ArtifactService service = new ArtifactService(mock(UploadService.class), signer,
                mock(DownloadTokenService.class), storage, Clock.fixed(now, ZoneOffset.UTC));
        service.setUnattachedFiles(unattached);
        Actor owner = new Actor(UUID.randomUUID(), Role.TEACHER);
        var ref = signer.sign(new FileRefSigner.Payload("file-1", owner.accountId(), ArtifactPurpose.MATERIAL,
                "application/pdf", 42, "de.pdf", "sha", now.plusSeconds(60).getEpochSecond()));

        assertThat(service.attach(ref, owner, ArtifactPurpose.MATERIAL).fileId()).isEqualTo("file-1");
        verify(unattached).markAttached("file-1");
    }
}
