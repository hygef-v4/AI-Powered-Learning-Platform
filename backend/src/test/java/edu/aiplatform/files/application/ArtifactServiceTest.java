package edu.aiplatform.files.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.files.contract.ArtifactPurpose;
import edu.aiplatform.files.contract.DownloadToken;
import edu.aiplatform.files.contract.FileRef;
import edu.aiplatform.files.contract.StoredFileInfo;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ArtifactServiceTest {

    final Instant now = Instant.parse("2026-10-16T08:00:00Z");
    final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    final FileRefSigner signer = new FileRefSigner(
            new FilesProperties("secret-of-at-least-32-bytes-long!!", false, null, null, null, null), new ObjectMapper());
    final DownloadTokenService tokens = mock(DownloadTokenService.class);
    final ArtifactService service = new ArtifactService(mock(UploadService.class), signer, tokens, mock(IStorage.class),
            clock);
    final Actor owner = new Actor(UUID.randomUUID(), Role.TEACHER);

    FileRef ref(UUID ownerId, ArtifactPurpose purpose, Instant expiresAt) {
        return signer.sign(new FileRefSigner.Payload("file-1", ownerId, purpose, "application/pdf", 42, "de.pdf",
                "sha", expiresAt.getEpochSecond()));
    }

    @Test
    void attachReturnsStoredFileForTheUploader() {
        StoredFileInfo info = service.attach(ref(owner.accountId(), ArtifactPurpose.MATERIAL, now.plusSeconds(60)),
                owner, ArtifactPurpose.MATERIAL);

        assertThat(info.fileId()).isEqualTo("file-1");
        assertThat(info.byteSize()).isEqualTo(42);
    }

    @Test
    void attachRejectsExpiredForeignOrWrongPurposeReferences() {
        assertThatThrownBy(() -> service.attach(ref(owner.accountId(), ArtifactPurpose.MATERIAL, now.minusSeconds(1)),
                owner, ArtifactPurpose.MATERIAL)).isInstanceOf(ApiException.class).hasMessageContaining("hết hạn");
        assertThatThrownBy(() -> service.attach(ref(UUID.randomUUID(), ArtifactPurpose.MATERIAL, now.plusSeconds(60)),
                owner, ArtifactPurpose.MATERIAL)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.attach(ref(owner.accountId(), ArtifactPurpose.MATERIAL, now.plusSeconds(60)),
                owner, ArtifactPurpose.DOCUMENT_IMAGE)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.attach(new FileRef("x.y"), owner, ArtifactPurpose.MATERIAL))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void downloadTokenLinkLastsFiveMinutes() {
        when(tokens.issue("file-1", owner.accountId())).thenReturn("tok");

        DownloadToken token = service.issueDownloadToken("file-1", owner.accountId());

        assertThat(token.url()).isEqualTo("/api/v1/files/download/tok");
        assertThat(token.expiresAt()).isEqualTo(now.plusSeconds(300));
    }
}
