package edu.aiplatform.files.application;

import edu.aiplatform.files.domain.FileNameSanitizer;
import edu.aiplatform.files.domain.FileRefClaims;
import edu.aiplatform.files.domain.PurposePolicy;
import edu.aiplatform.files.domain.StoredFile;
import edu.aiplatform.files.infrastructure.FileRefSigner;
import edu.aiplatform.files.port.ArtifactPurpose;
import edu.aiplatform.files.port.FileRef;
import edu.aiplatform.files.port.StoragePort;
import edu.aiplatform.files.port.StorageUnavailableException;
import edu.aiplatform.files.port.StorageWriteException;
import edu.aiplatform.files.port.UploadedFile;
import edu.aiplatform.identity.port.AuthorizationDecision;
import edu.aiplatform.identity.port.AuthorizationPort;
import edu.aiplatform.jobs.port.JobPort;
import edu.aiplatform.jobs.port.JobTypes;
import edu.aiplatform.shared.contract.ActorRef;
import edu.aiplatform.shared.contract.ResourceRef;
import edu.aiplatform.shared.web.ApiException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Upload (F1, P1): tối đa 5 upload cùng lúc; kiểm quyền, kích thước, loại tệp theo nội dung; SHA-256; đẩy tệp và
 * metadata qua {@link StoragePort}; lỗi giữa chừng thì dọn tệp đã tạo; luôn xóa tệp tạm (BR-U03-01…08, 41).
 */
@Service
public class UploadService {

    private static final Logger log = LoggerFactory.getLogger(UploadService.class);
    static final String UPLOAD_ACTION = "files.upload";
    static final int CLEANUP_ATTEMPTS = 3;

    private final StoragePort storage;
    private final ContentInspector inspector;
    private final FileRefSigner signer;
    private final AuthorizationPort authorization;
    private final FileAudit audit;
    private final JobPort jobs;
    private final Semaphore permits;
    private final Path tmpDir;
    private final Duration fileRefTtl;
    private final Clock clock;

    @Autowired
    public UploadService(StoragePort storage, ContentInspector inspector, FileRefSigner signer,
                         AuthorizationPort authorization, FileAudit audit, JobPort jobs, FilesProperties properties) {
        this(storage, inspector, signer, authorization, audit, jobs, properties.maxConcurrentUploads(),
                properties.uploadTmpDir(), properties.fileRefTtl(), Clock.systemUTC());
    }

    UploadService(StoragePort storage, ContentInspector inspector, FileRefSigner signer, AuthorizationPort authorization,
                  FileAudit audit, JobPort jobs, int maxConcurrent, Path tmpDir, Duration fileRefTtl, Clock clock) {
        this.storage = storage;
        this.inspector = inspector;
        this.signer = signer;
        this.authorization = authorization;
        this.audit = audit;
        this.jobs = jobs;
        this.permits = new Semaphore(maxConcurrent);
        this.tmpDir = tmpDir;
        this.fileRefTtl = fileRefTtl;
        this.clock = clock;
    }

    public UploadResult upload(ActorRef actor, ArtifactPurpose purpose, UploadedFile file) {
        if (!permits.tryAcquire()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "UPLOAD_BUSY",
                    "Hệ thống đang bận, vui lòng thử lại sau.");
        }
        Path tmp = null;
        try {
            PurposePolicy policy = PurposePolicy.of(purpose);
            authorize(actor, purpose, policy);
            if (file.byteSize() > policy.maxBytes()) {
                throw tooLarge(actor, purpose, policy);
            }
            String fileName = FileNameSanitizer.sanitize(file.originalFileName());
            tmp = Files.createTempFile(ensureTmpDir(), "upload-", ".part");
            CopyResult copy = copyWithLimit(file.content(), tmp, policy.maxBytes());
            if (copy == null) {
                throw tooLarge(actor, purpose, policy);
            }
            String mediaType = inspector.detect(tmp, fileName);
            if (!policy.allowsMediaType(mediaType)) {
                audit.uploadDenied(actor.accountId(), "FILE_TYPE_NOT_ALLOWED",
                        Map.of("purpose", purpose.name(), "mediaType", mediaType));
                throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "FILE_TYPE_NOT_ALLOWED",
                        "Loại tệp không được hỗ trợ cho mục đích này.");
            }
            String fileId = put(tmp, StoredFile.metadata(actor.accountId(), purpose, mediaType, fileName, copy.sha256()));
            Instant expiresAt = clock.instant().plus(fileRefTtl);
            FileRef ref = signer.sign(new FileRefClaims(fileId, actor.accountId(), purpose, expiresAt));
            return new UploadResult(ref, expiresAt, mediaType, copy.byteSize(), fileName);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            deleteQuietly(tmp);
            permits.release();
        }
    }

    private void authorize(ActorRef actor, ArtifactPurpose purpose, PurposePolicy policy) {
        if (actor == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Vui lòng đăng nhập.");
        }
        boolean allowed = policy.allowsRole(actor.role());
        String reason = "ROLE_NOT_ALLOWED";
        if (allowed) {
            AuthorizationDecision decision = authorization.authorize(actor, UPLOAD_ACTION,
                    new ResourceRef("FILE_PURPOSE_" + purpose.name(), null));
            allowed = decision.allowed();
            reason = decision.reasonCode();
        }
        if (!allowed) {
            audit.uploadDenied(actor.accountId(), reason, Map.of("purpose", purpose.name()));
            throw new ApiException(HttpStatus.FORBIDDEN, "FILE_UPLOAD_FORBIDDEN",
                    "Bạn không có quyền tải lên tệp cho mục đích này.");
        }
    }

    private ApiException tooLarge(ActorRef actor, ArtifactPurpose purpose, PurposePolicy policy) {
        audit.uploadDenied(actor.accountId(), "FILE_TOO_LARGE", Map.of("purpose", purpose.name()));
        return new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE",
                "Tệp vượt quá " + (policy.maxBytes() / PurposePolicy.MB) + " MB.");
    }

    private String put(Path tmp, Map<String, String> metadata) {
        try {
            return storage.put(tmp, metadata);
        } catch (StorageWriteException e) {
            if (e.orphanFileId() != null) {
                cleanup(e.orphanFileId());
            }
            throw unavailable(e);
        } catch (StorageUnavailableException e) {
            throw unavailable(e);
        }
    }

    /** Xóa tệp ghi dở, thử 3 lần; vẫn lỗi thì giao cho worker qua việc `DRIVE_CLEANUP` (BR-U03-06, P1). */
    void cleanup(String orphanFileId) {
        for (int i = 0; i < CLEANUP_ATTEMPTS; i++) {
            try {
                storage.delete(orphanFileId);
                return;
            } catch (RuntimeException e) {
                log.warn("Cleanup of partial upload failed (attempt {}): {}", i + 1, e.toString());
            }
        }
        // Khóa idempotent không chứa fileId để không lộ ID tệp trong log (NFR-U03-24).
        String key = "drive-cleanup:" + UUID.nameUUIDFromBytes(orphanFileId.getBytes(StandardCharsets.UTF_8));
        jobs.enqueue(JobTypes.DRIVE_CLEANUP, Map.of("fileId", orphanFileId), key);
    }

    private static ApiException unavailable(StorageUnavailableException e) {
        log.warn("Storage unavailable during upload: {}", e.getMessage());
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "STORAGE_UNAVAILABLE",
                "Lưu trữ tệp tạm thời không khả dụng, vui lòng thử lại sau.");
    }

    private record CopyResult(long byteSize, String sha256) {}

    /** Ghi ra tệp tạm, đếm byte và tính SHA-256; `null` khi vượt trần (không đọc tiếp). */
    private static CopyResult copyWithLimit(InputStream in, Path target, long maxBytes) throws IOException {
        MessageDigest digest = sha256();
        long total = 0;
        byte[] buffer = new byte[64 * 1024];
        try (InputStream source = in; OutputStream out = new DigestOutputStream(Files.newOutputStream(target), digest)) {
            int n;
            while ((n = source.read(buffer)) != -1) {
                total += n;
                if (total > maxBytes) {
                    return null;
                }
                out.write(buffer, 0, n);
            }
        }
        return new CopyResult(total, HexFormat.of().formatHex(digest.digest()));
    }

    private Path ensureTmpDir() throws IOException {
        return Files.createDirectories(tmpDir);
    }

    private static void deleteQuietly(Path tmp) {
        if (tmp == null) {
            return;
        }
        try {
            Files.deleteIfExists(tmp);
        } catch (IOException e) {
            log.warn("Could not delete temp upload: {}", e.toString());
        }
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
