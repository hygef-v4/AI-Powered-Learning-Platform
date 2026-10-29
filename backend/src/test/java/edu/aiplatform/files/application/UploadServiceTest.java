package edu.aiplatform.files.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.files.contract.ArtifactPurpose;
import edu.aiplatform.files.contract.FileUpload;
import edu.aiplatform.files.domain.PurposePolicy;
import edu.aiplatform.files.infrastructure.LocalFolderStorageAdapter;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.AuthorizationDecision;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.jobs.contract.IJob;
import edu.aiplatform.jobs.contract.JobTypes;
import edu.aiplatform.settings.contract.ISettings;
import edu.aiplatform.shared.error.ApiException;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;

class UploadServiceTest {

    @TempDir
    Path dir;

    final IAuthorization authorization = mock(IAuthorization.class);
    final ISettings settings = mock(ISettings.class);
    final FileAudit fileAudit = mock(FileAudit.class);
    final IJob jobs = mock(IJob.class);
    final Clock clock = Clock.fixed(Instant.parse("2026-10-16T08:00:00Z"), ZoneOffset.UTC);
    final Actor teacher = new Actor(UUID.randomUUID(), Role.TEACHER);
    IStorage storage;
    FileRefSigner signer;
    UploadService service;

    @BeforeEach
    void setUp() {
        when(authorization.authorize(any(), any(), any())).thenReturn(AuthorizationDecision.allow());
        when(settings.get(any())).thenReturn(Optional.empty());
        storage = spy(new LocalFolderStorageAdapter(dir.resolve("store")));
        FilesProperties props = new FilesProperties("secret-of-at-least-32-bytes-long!!", false, null,
                dir.resolve("tmp").toString(), null, null);
        signer = new FileRefSigner(props, new ObjectMapper());
        service = new UploadService(authorization, new PurposePolicy(settings), new ContentInspector(), storage, signer,
                fileAudit, jobs, clock, props);
    }

    static FileUpload upload(String name, byte[] content) {
        return new FileUpload(name, content.length, new ByteArrayInputStream(content));
    }

    @Test
    void storesFileAndReturnsSignedReferenceValidForOneHour() {
        UploadResult result = service.upload(teacher, ArtifactPurpose.MATERIAL, upload("../de thi.pdf", TestFiles.PDF));

        assertThat(result.mediaType()).isEqualTo("application/pdf");
        assertThat(result.originalFileName()).isEqualTo("de thi.pdf");
        assertThat(result.expiresAt()).isEqualTo(Instant.parse("2026-10-16T09:00:00Z"));
        FileRefSigner.Payload payload = signer.verify(result.fileRef()).orElseThrow();
        assertThat(payload.ownerAccountId()).isEqualTo(teacher.accountId());
        assertThat(payload.sha256()).hasSize(64);
        assertThat(storage.metadata(payload.fileId())).isPresent();
        assertThat(dir.resolve("tmp")).isEmptyDirectory();
    }

    @Test
    void adminCannotUploadAndTheDenialIsAudited() {
        Actor admin = new Actor(UUID.randomUUID(), Role.ADMIN);

        assertThatThrownBy(() -> service.upload(admin, ArtifactPurpose.MATERIAL, upload("a.pdf", TestFiles.PDF)))
                .isInstanceOf(ApiException.class).extracting("status").isEqualTo(HttpStatus.FORBIDDEN);
        verify(fileAudit).uploadDenied(eq(admin.accountId()), eq("ROLE_NOT_ALLOWED"), anyMap());
    }

    @Test
    void rejectsFileOverTheLimitFromSettings() {
        when(settings.get(PurposePolicy.IMAGE_MAX_MB)).thenReturn(Optional.of(1));
        byte[] big = new byte[1024 * 1024 + 1];
        System.arraycopy(TestFiles.PNG, 0, big, 0, TestFiles.PNG.length);

        assertThatThrownBy(() -> service.upload(teacher, ArtifactPurpose.DOCUMENT_IMAGE, upload("a.png", big)))
                .isInstanceOf(ApiException.class).extracting("status").isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @Test
    void stopsReadingWhenTheStreamIsLongerThanDeclared() {
        when(settings.get(PurposePolicy.IMAGE_MAX_MB)).thenReturn(Optional.of(1));
        byte[] big = new byte[2 * 1024 * 1024];
        FileUpload lying = new FileUpload("a.png", 10, new ByteArrayInputStream(big));

        assertThatThrownBy(() -> service.upload(teacher, ArtifactPurpose.DOCUMENT_IMAGE, lying))
                .isInstanceOf(ApiException.class).extracting("status").isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        verify(storage, never()).put(any(), any(), any());
    }

    @Test
    void rejectsRenamedFileAndAuditsIt() {
        assertThatThrownBy(() -> service.upload(teacher, ArtifactPurpose.MATERIAL, upload("anh.pdf", TestFiles.PNG)))
                .isInstanceOf(ApiException.class).extracting("status").isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        verify(fileAudit).uploadDenied(eq(teacher.accountId()), eq("FILE_TYPE_NOT_ALLOWED"), anyMap());
    }

    @Test
    void rejectsTypeTurnedOffInSettings() {
        when(settings.get(PurposePolicy.IMAGE_TYPES)).thenReturn(Optional.of(new String[]{"PNG"}));

        assertThatThrownBy(() -> service.upload(teacher, ArtifactPurpose.DOCUMENT_IMAGE, upload("a.svg", TestFiles.SVG)))
                .isInstanceOf(ApiException.class).extracting("status").isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    void reportsUnavailableWhenStorageIsDown() {
        IStorage down = mock(IStorage.class);
        when(down.available()).thenReturn(false);
        FilesProperties props = new FilesProperties("secret-of-at-least-32-bytes-long!!", false, null,
                dir.resolve("tmp").toString(), null, null);
        UploadService offline = new UploadService(authorization, new PurposePolicy(settings), new ContentInspector(),
                down, signer, fileAudit, jobs, clock, props);

        assertThatThrownBy(() -> offline.upload(teacher, ArtifactPurpose.MATERIAL, upload("a.pdf", TestFiles.PDF)))
                .isInstanceOf(ApiException.class).extracting("status").isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void enqueuesCleanupWhenDeletingAPartialFileKeepsFailing() {
        doThrow(new StorageUnavailableException("down", null)).when(storage).delete(anyString());

        service.compensate("local-00000000-0000-0000-0000-000000000000");

        verify(storage, times(UploadService.DELETE_ATTEMPTS)).delete(anyString());
        verify(jobs).enqueue(eq(JobTypes.DRIVE_CLEANUP), anyMap(), anyString());
    }

    @Test
    void deletesTheTemporaryFileEvenWhenRejected() throws Exception {
        try {
            service.upload(teacher, ArtifactPurpose.MATERIAL, upload("anh.pdf", TestFiles.PNG));
        } catch (ApiException ignored) {
            // Mong đợi.
        }
        try (var files = Files.list(dir.resolve("tmp"))) {
            assertThat(files).isEmpty();
        }
    }
}
