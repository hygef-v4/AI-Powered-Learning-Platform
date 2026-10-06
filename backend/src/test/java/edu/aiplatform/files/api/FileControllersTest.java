package edu.aiplatform.files.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.aiplatform.audit.port.AuditPort;
import edu.aiplatform.files.FileFixtures;
import edu.aiplatform.files.FileFixtures.MemoryStorage;
import edu.aiplatform.files.application.ContentInspector;
import edu.aiplatform.files.application.DownloadTokenService;
import edu.aiplatform.files.application.FileAudit;
import edu.aiplatform.files.application.UploadService;
import edu.aiplatform.files.domain.StoredFile;
import edu.aiplatform.files.infrastructure.FileRefSigner;
import edu.aiplatform.files.port.ArtifactPurpose;
import edu.aiplatform.files.port.FileUnavailableException;
import edu.aiplatform.identity.port.AuthorizationDecision;
import edu.aiplatform.identity.port.AuthorizationPort;
import edu.aiplatform.jobs.port.JobPort;
import edu.aiplatform.shared.contract.ActorRef;
import edu.aiplatform.shared.contract.Role;
import edu.aiplatform.shared.web.GlobalExceptionHandler;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Bước 22: không trả `fileId`, header tải về đúng (CSP cho SVG), sai `purpose`/vai trò bị từ chối, quá lớn `413`. */
class FileControllersTest {

    @TempDir
    Path tmp;

    private final AuthorizationPort authorization = mock(AuthorizationPort.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final ActorRef student = new ActorRef(UUID.randomUUID(), Role.STUDENT);
    private MemoryStorage storage;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        storage = new MemoryStorage();
        when(redis.opsForValue()).thenReturn(values);
        when(authorization.authorize(any(), anyString(), any()))
                .thenReturn(new AuthorizationDecision(true, "OK", null, null, null));
        FileAudit audit = new FileAudit(mock(AuditPort.class));
        UploadService uploads = new UploadServiceFactory(storage, authorization, audit, tmp).create();
        DownloadTokenService tokens = new DownloadTokenServiceFactory(redis, audit).create();
        mvc = MockMvcBuilders.standaloneSetup(new FileUploadController(uploads),
                        new DownloadController(tokens, storage, audit))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(student, null, List.of()));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void uploadReturnsFileRefButNeverFileId() throws Exception {
        mvc.perform(multipart("/api/v1/files").file(png()).param("purpose", "DOCUMENT_IMAGE"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileRef").isString())
                .andExpect(jsonPath("$.mediaType").value("image/png"))
                .andExpect(jsonPath("$.fileId").doesNotExist())
                .andExpect(jsonPath("$.providerFileId").doesNotExist());
    }

    @Test
    void unknownPurposeAndForbiddenRoleAreRejected() throws Exception {
        mvc.perform(multipart("/api/v1/files").file(png()).param("purpose", "EXECUTABLE"))
                .andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/v1/files").file(png()).param("purpose", "MATERIAL"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FILE_UPLOAD_FORBIDDEN"));
    }

    @Test
    void oversizedImageIs413() throws Exception {
        byte[] big = new byte[5 * 1024 * 1024 + 1];
        mvc.perform(multipart("/api/v1/files").file(new MockMultipartFile("file", "a.png", "image/png", big))
                        .param("purpose", "DOCUMENT_IMAGE"))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("FILE_TOO_LARGE"));
    }

    @Test
    void svgDownloadIsSandboxedAndNotCached() throws Exception {
        String id = store("image/svg+xml", "sơ đồ.svg", FileFixtures.svgWithScript());
        when(values.get(any())).thenReturn(id + "|" + student.accountId());

        MvcResult started = mvc.perform(get("/api/v1/files/download/tok"))
                .andExpect(request().asyncStarted()).andReturn();
        mvc.perform(asyncDispatch(started))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/svg+xml"))
                .andExpect(header().string("Content-Security-Policy", DownloadController.SVG_CSP))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Cache-Control", "no-store, private"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.startsWith("inline;"),
                        org.hamcrest.Matchers.containsString("filename*=UTF-8''s%C6%A1%20%C4%91%E1%BB%93.svg"))))
                .andExpect(content().bytes(FileFixtures.svgWithScript()));
    }

    @Test
    void nonImageDownloadIsAttachment() throws Exception {
        String id = store("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "bai.docx",
                FileFixtures.docx());
        when(values.get(any())).thenReturn(id + "|" + student.accountId());

        MvcResult started = mvc.perform(get("/api/v1/files/download/tok")).andReturn();
        mvc.perform(asyncDispatch(started))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("attachment")));
    }

    @Test
    void abusiveFileIs410() throws Exception {
        MemoryStorage blocked = new MemoryStorage() {
            @Override
            public InputStream get(String fileId) {
                throw new FileUnavailableException("abuse");
            }
        };
        blocked.meta.put("x", Map.of("mediaType", "application/pdf"));
        when(values.get(any())).thenReturn("x|" + student.accountId());
        FileAudit audit = new FileAudit(mock(AuditPort.class));
        MockMvc blockedMvc = MockMvcBuilders.standaloneSetup(
                        new DownloadController(new DownloadTokenServiceFactory(redis, audit).create(), blocked, audit))
                .setControllerAdvice(new GlobalExceptionHandler()).build();

        blockedMvc.perform(get("/api/v1/files/download/tok"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("FILE_UNAVAILABLE"));
    }

    @Test
    void storageOutageIs503() throws Exception {
        FileAudit audit = new FileAudit(mock(AuditPort.class));
        when(values.get(any())).thenReturn("x|" + student.accountId());
        MockMvc down = MockMvcBuilders.standaloneSetup(new DownloadController(
                        new DownloadTokenServiceFactory(redis, audit).create(),
                        new edu.aiplatform.files.infrastructure.UnconfiguredStorageAdapter(), audit))
                .setControllerAdvice(new GlobalExceptionHandler()).build();

        down.perform(get("/api/v1/files/download/tok"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("STORAGE_UNAVAILABLE"));
    }

    @Test
    void tokenOfAnotherAccountIs404() throws Exception {
        when(values.get(any())).thenReturn("x|" + UUID.randomUUID());
        mvc.perform(get("/api/v1/files/download/tok")).andExpect(status().isNotFound());
    }

    private String store(String mediaType, String name, byte[] bytes) throws Exception {
        return storage.put(Files.write(tmp.resolve("s" + System.nanoTime()), bytes),
                StoredFile.metadata(student.accountId(), ArtifactPurpose.DOCUMENT_IMAGE, mediaType, name, "x"));
    }

    private static MockMultipartFile png() {
        return new MockMultipartFile("file", "anh.png", "image/png", FileFixtures.png());
    }

    /** Dựng service bằng constructor package-private trong cùng package `application`. */
    private record UploadServiceFactory(MemoryStorage storage, AuthorizationPort authorization, FileAudit audit,
                                        Path tmp) {
        UploadService create() {
            return edu.aiplatform.files.application.TestFactories.upload(storage, new ContentInspector(),
                    new FileRefSigner("s", Clock.systemUTC()), authorization, audit, mock(JobPort.class), tmp);
        }
    }

    private record DownloadTokenServiceFactory(StringRedisTemplate redis, FileAudit audit) {
        DownloadTokenService create() {
            return edu.aiplatform.files.application.TestFactories.tokens(redis, audit, Duration.ofMinutes(5));
        }
    }
}
