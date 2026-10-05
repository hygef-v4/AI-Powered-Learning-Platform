package edu.aiplatform.files.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.aiplatform.audit.port.AuditEvent;
import edu.aiplatform.audit.port.AuditPort;
import edu.aiplatform.audit.port.AuditResult;
import edu.aiplatform.files.FileFixtures;
import edu.aiplatform.files.FileFixtures.MemoryStorage;
import edu.aiplatform.files.domain.FileRefClaims;
import edu.aiplatform.files.infrastructure.FileRefSigner;
import edu.aiplatform.files.port.ArtifactPurpose;
import edu.aiplatform.files.port.StoragePort;
import edu.aiplatform.files.port.UploadedFile;
import edu.aiplatform.identity.port.AuthorizationDecision;
import edu.aiplatform.identity.port.AuthorizationPort;
import edu.aiplatform.jobs.port.JobPort;
import edu.aiplatform.jobs.port.JobTypes;
import edu.aiplatform.shared.contract.ActorRef;
import edu.aiplatform.shared.contract.Role;
import edu.aiplatform.shared.web.ApiException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

/** BR-U03-01…08, 40, 41; NFR-U03-01, 02; P1. */
class UploadServiceTest {

    @TempDir
    Path tmpDir;

    private final MemoryStorage storage = new MemoryStorage();
    private final AuthorizationPort authorization = mock(AuthorizationPort.class);
    private final AuditPort auditPort = mock(AuditPort.class);
    private final JobPort jobs = mock(JobPort.class);
    private final FileRefSigner signer = new FileRefSigner("secret-for-tests", Clock.systemUTC());
    private final ActorRef teacher = new ActorRef(UUID.randomUUID(), Role.TEACHER);
    private final ActorRef student = new ActorRef(UUID.randomUUID(), Role.STUDENT);
    private UploadService service;

    @BeforeEach
    void setUp() {
        when(authorization.authorize(any(), anyString(), any()))
                .thenAnswer(inv -> new AuthorizationDecision(true, "OK", null, null, null));
        service = service(storage, 5);
    }

    @Test
    void storesFileWithMetadataAndReturnsSignedRef() throws IOException {
        UploadResult result = service.upload(teacher, ArtifactPurpose.MATERIAL, file("../Bài giảng.pdf",
                FileFixtures.pdf()));

        assertThat(result.mediaType()).isEqualTo("application/pdf");
        assertThat(result.originalFileName()).isEqualTo("Bài giảng.pdf");
        FileRefClaims claims = signer.verify(result.fileRef()).orElseThrow();
        Map<String, String> meta = storage.meta.get(claims.fileId());
        assertThat(meta).containsEntry(StoragePort.OWNER_ACCOUNT_ID, teacher.accountId().toString())
                .containsEntry(StoragePort.PURPOSE, "MATERIAL")
                .containsEntry(StoragePort.MEDIA_TYPE, "application/pdf")
                .containsEntry(StoragePort.ORIGINAL_FILE_NAME, "Bài giảng.pdf")
                .containsKey(StoragePort.SHA256);
        assertThat(meta.get(StoragePort.SHA256)).hasSize(64);
        assertNoTempFiles();
    }

    @Test
    void renamedExecutableIsRejectedAndAudited() throws IOException {
        assertStatus(() -> service.upload(student, ArtifactPurpose.DOCUMENT_IMAGE, file("anh.png", FileFixtures.exe())),
                HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(storage.data).isEmpty();
        assertDeniedAudit("FILE_TYPE_NOT_ALLOWED");
        assertNoTempFiles();
    }

    @Test
    void sizeCapsPerPurposeIncludingLyingDeclaredSize() {
        byte[] sixMb = new byte[6 * 1024 * 1024];
        System.arraycopy(FileFixtures.png(), 0, sixMb, 0, FileFixtures.png().length);
        assertStatus(() -> service.upload(student, ArtifactPurpose.DOCUMENT_IMAGE,
                new UploadedFile("a.png", "image/png", sixMb.length, new ByteArrayInputStream(sixMb))),
                HttpStatus.PAYLOAD_TOO_LARGE);
        // Khai báo nhỏ nhưng nội dung thật vượt trần: vẫn bị chặn khi đếm byte.
        assertStatus(() -> service.upload(student, ArtifactPurpose.DOCUMENT_IMAGE,
                new UploadedFile("a.png", "image/png", 10, new ByteArrayInputStream(sixMb))),
                HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(storage.data).isEmpty();
    }

    @Test
    void roleNotAllowedForPurposeIsForbidden() throws IOException {
        assertStatus(() -> service.upload(student, ArtifactPurpose.MATERIAL, file("a.pdf", FileFixtures.pdf())),
                HttpStatus.FORBIDDEN);
        verify(authorization, never()).authorize(any(), anyString(), any());
        assertDeniedAudit("ROLE_NOT_ALLOWED");
    }

    @Test
    void authorizationPortDenialIsForbidden() throws IOException {
        when(authorization.authorize(any(), anyString(), any()))
                .thenReturn(new AuthorizationDecision(false, "ACCOUNT_DISABLED", null, null, null));
        assertStatus(() -> service.upload(teacher, ArtifactPurpose.MATERIAL, file("a.pdf", FileFixtures.pdf())),
                HttpStatus.FORBIDDEN);
        assertDeniedAudit("ACCOUNT_DISABLED");
    }

    @Test
    void sixthConcurrentUploadGets503() throws Exception {
        CountDownLatch inPut = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        MemoryStorage slow = new MemoryStorage() {
            @Override
            public String put(Path content, Map<String, String> metadata) {
                inPut.countDown();
                try {
                    release.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return super.put(content, metadata);
            }
        };
        UploadService oneSlot = service(slow, 1);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<UploadResult> first = pool.submit(() -> oneSlot.upload(teacher, ArtifactPurpose.MATERIAL,
                    file("a.pdf", FileFixtures.pdf())));
            assertThat(inPut.await(5, TimeUnit.SECONDS)).isTrue();
            assertStatus(() -> oneSlot.upload(teacher, ArtifactPurpose.MATERIAL, file("b.pdf", FileFixtures.pdf())),
                    HttpStatus.SERVICE_UNAVAILABLE);
            release.countDown();
            assertThat(first.get(5, TimeUnit.SECONDS).fileRef()).isNotNull();
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void partialWriteIsCleanedUpImmediately() throws IOException {
        storage.failMetadataWrite = true;
        assertStatus(() -> service.upload(teacher, ArtifactPurpose.MATERIAL, file("a.pdf", FileFixtures.pdf())),
                HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(storage.data).isEmpty();
        verify(jobs, never()).enqueue(anyString(), any(), anyString());
        assertNoTempFiles();
    }

    @Test
    void cleanupFallsBackToDriveCleanupJobAfterThreeFailures() throws IOException {
        storage.failMetadataWrite = true;
        storage.deleteFailures = 3;
        assertStatus(() -> service.upload(teacher, ArtifactPurpose.MATERIAL, file("a.pdf", FileFixtures.pdf())),
                HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(storage.deleteCalls).isEqualTo(3);
        String orphan = storage.data.keySet().iterator().next();
        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(jobs).enqueue(eq(JobTypes.DRIVE_CLEANUP), eq(Map.of("fileId", orphan)), key.capture());
        assertThat(key.getValue()).startsWith("drive-cleanup:").doesNotContain(orphan);
    }

    private UploadService service(StoragePort store, int maxConcurrent) {
        return new UploadService(store, new ContentInspector(), signer, authorization, new FileAudit(auditPort), jobs,
                maxConcurrent, tmpDir, Duration.ofHours(1), Clock.systemUTC());
    }

    private static UploadedFile file(String name, byte[] content) {
        InputStream in = new ByteArrayInputStream(content);
        return new UploadedFile(name, "application/octet-stream", content.length, in);
    }

    private static void assertStatus(Runnable call, HttpStatus status) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.status()).isEqualTo(status));
    }

    private void assertDeniedAudit(String reason) {
        ArgumentCaptor<AuditEvent> event = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPort).recordDenied(event.capture());
        assertThat(event.getValue().result()).isEqualTo(AuditResult.DENIED);
        assertThat(event.getValue().reason()).isEqualTo(reason);
        assertThat(event.getValue().objectId()).isNull();
    }

    private void assertNoTempFiles() throws IOException {
        try (var files = Files.list(tmpDir)) {
            assertThat(files).isEmpty();
        }
    }
}
