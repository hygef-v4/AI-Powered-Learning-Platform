package edu.aiplatform.files.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.aiplatform.files.contract.ArtifactPurpose;
import edu.aiplatform.files.contract.IArtifactDownloadPolicy;
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
    final IArtifactDownloadPolicy policy = mock(IArtifactDownloadPolicy.class);
    final DownloadService service = new DownloadService(tokens, storage, java.util.List.of(policy));
    final Actor student = new Actor(UUID.randomUUID(), Role.STUDENT);

    @org.junit.jupiter.api.BeforeEach
    void setupPolicy() {
        when(policy.purpose()).thenReturn(ArtifactPurpose.MATERIAL);
    }

    @Test
    void materialDownloadFailsClosedWithoutAnOwnerPolicy() {
        when(tokens.resolve("tok")).thenReturn(Optional.of(new DownloadTokenService.Grant("f", student.accountId())));
        when(storage.loadMetadata("f")).thenReturn(Optional.of(meta("application/pdf", "a.pdf")));
        assertThatThrownBy(() -> new DownloadService(tokens, storage).open(student, "tok"))
                .isInstanceOfSatisfying(ApiException.class, failure -> assertThat(failure.readCode()).isEqualTo("FILE_ACCESS_UNAVAILABLE"));
        org.mockito.Mockito.verify(storage, org.mockito.Mockito.never()).open("f");
    }

    @Test
    void rechecksMaterialPolicyBeforeOpeningPreviouslyGrantedFile() {
        when(tokens.resolve("tok")).thenReturn(Optional.of(new DownloadTokenService.Grant("f", student.accountId())));
        when(storage.loadMetadata("f")).thenReturn(Optional.of(meta("application/pdf", "a.pdf")));
        org.mockito.Mockito.doThrow(ApiException.createNotFound("MATERIAL_NOT_FOUND"))
                .when(policy).requireDownload(student, "f");
        assertThatThrownBy(() -> service.open(student, "tok"))
                .isInstanceOfSatisfying(ApiException.class, failure -> assertThat(failure.readCode()).isEqualTo("MATERIAL_NOT_FOUND"));
        org.mockito.Mockito.verify(storage, org.mockito.Mockito.never()).open("f");
    }

    static StoredMetadata meta(String mediaType, String name) {
        return new StoredMetadata(UUID.randomUUID(), ArtifactPurpose.MATERIAL, name, mediaType, 1, "sha");
    }

    @Test
    void tokenOfAnotherAccountLooksLikeNotFoundAndIsAudited() {
        when(tokens.resolve("tok")).thenReturn(Optional.of(new DownloadTokenService.Grant("f", UUID.randomUUID())));

        assertThatThrownBy(() -> service.open(student, "tok")).isInstanceOf(ApiException.class)
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void abusiveFileIsGoneAndAudited() {
        when(tokens.resolve("tok")).thenReturn(Optional.of(new DownloadTokenService.Grant("f", student.accountId())));
        when(storage.loadMetadata("f")).thenReturn(Optional.of(meta("application/pdf", "a.pdf")));
        when(storage.open("f")).thenThrow(new FileAbusiveException("f"));

        assertThatThrownBy(() -> service.open(student, "tok")).isInstanceOf(ApiException.class)
                .extracting("status").isEqualTo(HttpStatus.GONE);
    }

    @Test
    void svgIsSandboxedAndNeverSniffed() {
        HttpHeaders headers = DownloadService.buildHeaders(meta("image/svg+xml", "so-do.svg"));

        assertThat(headers.getFirst("Content-Security-Policy")).isEqualTo(DownloadService.SVG_CSP);
        assertThat(headers.getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(headers.getCacheControl()).isEqualTo("private, no-store");
        assertThat(headers.getContentDisposition().isInline()).isTrue();
    }

    @Test
    void pdfOpensInlineButOfficeFilesDownload() {
        assertThat(DownloadService.buildHeaders(meta("application/pdf", "a.pdf")).getContentDisposition().isInline()).isTrue();
        HttpHeaders docx = DownloadService.buildHeaders(meta(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "bài.docx"));
        assertThat(docx.getContentDisposition().isAttachment()).isTrue();
        assertThat(docx.getContentDisposition().getFilename()).isEqualTo("bài.docx");
        assertThat(docx.getFirst("Content-Security-Policy")).isNull();
    }
}
