package edu.aiplatform.files.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.aiplatform.files.contract.ArtifactPurpose;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.shared.error.ApiException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

class DownloadServiceTest {

    final DownloadTokenService tokens = mock(DownloadTokenService.class);
    final IStorage storage = mock(IStorage.class);
    final FileAudit fileAudit = mock(FileAudit.class);
    final DownloadService service = new DownloadService(tokens, storage, fileAudit);
    final Actor student = new Actor(UUID.randomUUID(), Role.STUDENT);

    static StoredMetadata meta(String mediaType, String name) {
        return new StoredMetadata(UUID.randomUUID(), ArtifactPurpose.MATERIAL, name, mediaType, 1, "sha");
    }

    @Test
    void tokenOfAnotherAccountLooksLikeNotFoundAndIsAudited() {
        when(tokens.resolve("tok")).thenReturn(Optional.of(new DownloadTokenService.Grant("f", UUID.randomUUID())));

        assertThatThrownBy(() -> service.open(student, "tok")).isInstanceOf(ApiException.class)
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
        verify(fileAudit).downloadWrongAccount(student.accountId());
    }

    @Test
    void abusiveFileIsGoneAndAudited() {
        when(tokens.resolve("tok")).thenReturn(Optional.of(new DownloadTokenService.Grant("f", student.accountId())));
        when(storage.metadata("f")).thenReturn(Optional.of(meta("application/pdf", "a.pdf")));
        when(storage.open("f")).thenThrow(new FileAbusiveException("f"));

        assertThatThrownBy(() -> service.open(student, "tok")).isInstanceOf(ApiException.class)
                .extracting("status").isEqualTo(HttpStatus.GONE);
        verify(fileAudit).downloadAbusive(student.accountId());
    }

    @Test
    void svgIsSandboxedAndNeverSniffed() {
        HttpHeaders headers = DownloadService.headers(meta("image/svg+xml", "so-do.svg"));

        assertThat(headers.getFirst("Content-Security-Policy")).isEqualTo(DownloadService.SVG_CSP);
        assertThat(headers.getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(headers.getCacheControl()).isEqualTo("private, no-store");
        assertThat(headers.getContentDisposition().isInline()).isTrue();
    }

    @Test
    void pdfOpensInlineButOfficeFilesDownload() {
        assertThat(DownloadService.headers(meta("application/pdf", "a.pdf")).getContentDisposition().isInline()).isTrue();
        HttpHeaders docx = DownloadService.headers(meta(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "bài.docx"));
        assertThat(docx.getContentDisposition().isAttachment()).isTrue();
        assertThat(docx.getContentDisposition().getFilename()).isEqualTo("bài.docx");
        assertThat(docx.getFirst("Content-Security-Policy")).isNull();
    }
}
