package edu.aiplatform.files.application;

import edu.aiplatform.files.contract.ArtifactPurpose;
import edu.aiplatform.files.contract.FileUpload;
import edu.aiplatform.files.domain.FileNameSanitizer;
import edu.aiplatform.files.domain.FileType;
import edu.aiplatform.files.domain.PurposeLimits;
import edu.aiplatform.files.domain.PurposePolicy;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.ResourceRef;
import edu.aiplatform.jobs.contract.IJob;
import edu.aiplatform.jobs.contract.JobTypes;
import edu.aiplatform.shared.error.ApiException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** F1 Upload (P1): ghi ra tệp tạm, kiểm trần và loại theo Settings, lưu lên Drive, trả {@code FileRef}. */
@Service
public class UploadService {

    static final Duration FILE_REF_TTL = Duration.ofHours(1);
    static final int MAX_CONCURRENT_UPLOADS = 5;
    static final int DELETE_ATTEMPTS = 3;

    private static final Logger log = LoggerFactory.getLogger(UploadService.class);

    private final IAuthorization authorization;
    private final PurposePolicy policy;
    private final ContentInspector inspector;
    private final IStorage storage;
    private final FileRefSigner signer;
    private final FileAudit fileAudit;
    private final IJob jobs;
    private final Clock clock;
    private final Path tmpDir;
    private final Semaphore permits = new Semaphore(MAX_CONCURRENT_UPLOADS);

    public UploadService(IAuthorization authorization, PurposePolicy policy, ContentInspector inspector,
                         IStorage storage, FileRefSigner signer, FileAudit fileAudit, IJob jobs, Clock clock,
                         FilesProperties properties) {
        this.authorization = authorization;
        this.policy = policy;
        this.inspector = inspector;
        this.storage = storage;
        this.signer = signer;
        this.fileAudit = fileAudit;
        this.jobs = jobs;
        this.clock = clock;
        this.tmpDir = Path.of(properties.uploadTmpDir() == null || properties.uploadTmpDir().isBlank()
                ? System.getProperty("java.io.tmpdir") : properties.uploadTmpDir());
    }

    public UploadResult upload(Actor actor, ArtifactPurpose purpose, FileUpload file) {
        if (!permits.tryAcquire()) {
            throw ApiException.unavailable("UPLOAD_BUSY", "Hệ thống đang bận, thử lại sau");
        }
        Path temp = null;
        try {
            checkAllowed(actor, purpose);
            PurposeLimits limits = policy.limits(purpose);
            String name = FileNameSanitizer.sanitize(file.originalFileName());
            if (file.byteSize() > limits.maxBytes()) {
                throw tooLarge(actor, purpose, limits);
            }
            Files.createDirectories(tmpDir);
            temp = Files.createTempFile(tmpDir, "upload-", ".tmp");
            CopyResult copy = copyWithLimit(file.content(), temp, limits.maxBytes());
            if (copy.tooLarge()) {
                throw tooLarge(actor, purpose, limits);
            }
            Optional<FileType> type = inspector.detect(temp, name);
            if (type.isEmpty() || !limits.allowedTypes().contains(type.get())) {
                fileAudit.uploadDenied(actor.accountId(), "FILE_TYPE_NOT_ALLOWED",
                        Map.of("purpose", purpose.name(), "detected", type.map(Enum::name).orElse("UNKNOWN")));
                throw ApiException.unsupportedMediaType("FILE_TYPE_NOT_ALLOWED", "Loại tệp không được phép");
            }
            if (!storage.available()) {
                throw ApiException.unavailable("STORAGE_UNAVAILABLE", "Lưu trữ tệp tạm thời không khả dụng");
            }
            StoredMetadata metadata = new StoredMetadata(actor.accountId(), purpose, name, type.get().mediaType(),
                    copy.bytes(), copy.sha256());
            String fileId = store(purpose, temp, metadata);
            return sign(fileId, metadata);
        } catch (IOException e) {
            throw ApiException.unavailable("UPLOAD_FAILED", "Không nhận được tệp, thử lại sau");
        } finally {
            deleteQuietly(temp);
            permits.release();
        }
    }

    private void checkAllowed(Actor actor, ArtifactPurpose purpose) {
        if (!authorization.authorize(actor, "FILE_UPLOAD", ResourceRef.none()).allowed()
                || !policy.canUpload(actor.role(), purpose)) {
            fileAudit.uploadDenied(actor.accountId(), "ROLE_NOT_ALLOWED", Map.of("purpose", purpose.name()));
            throw ApiException.forbidden("FILE_UPLOAD_FORBIDDEN");
        }
    }

    private ApiException tooLarge(Actor actor, ArtifactPurpose purpose, PurposeLimits limits) {
        fileAudit.uploadDenied(actor.accountId(), "FILE_TOO_LARGE",
                Map.of("purpose", purpose.name(), "maxSizeMb", limits.maxSizeMb()));
        return ApiException.payloadTooLarge("FILE_TOO_LARGE", "Tệp vượt " + limits.maxSizeMb() + " MB");
    }

    private String store(ArtifactPurpose purpose, Path temp, StoredMetadata metadata) {
        try {
            return storage.put(purpose, temp, metadata);
        } catch (StorageUnavailableException e) {
            throw ApiException.unavailable("STORAGE_UNAVAILABLE", "Lưu trữ tệp tạm thời không khả dụng");
        }
    }

    private UploadResult sign(String fileId, StoredMetadata metadata) {
        try {
            Instant expiresAt = clock.instant().plus(FILE_REF_TTL);
            var payload = new FileRefSigner.Payload(fileId, metadata.ownerAccountId(), metadata.purpose(),
                    metadata.mediaType(), metadata.byteSize(), metadata.originalFileName(), metadata.sha256(),
                    expiresAt.getEpochSecond());
            return new UploadResult(signer.sign(payload), metadata.mediaType(), metadata.byteSize(),
                    metadata.originalFileName(), expiresAt);
        } catch (RuntimeException e) {
            compensate(fileId);
            throw e;
        }
    }

    /** Xóa tệp đã lên Drive khi bước sau lỗi; xóa không được thì gửi việc dọn (BR-U03-06). */
    void compensate(String fileId) {
        for (int attempt = 1; attempt <= DELETE_ATTEMPTS; attempt++) {
            try {
                storage.delete(fileId);
                return;
            } catch (RuntimeException e) {
                log.warn("Xóa tệp dở dang lỗi, lần {}", attempt);
            }
        }
        jobs.enqueue(JobTypes.DRIVE_CLEANUP, Map.of("storage", "FILES", "ref", fileId), "cleanup:" + fileId);
    }

    record CopyResult(long bytes, String sha256, boolean tooLarge) {
    }

    static CopyResult copyWithLimit(InputStream in, Path target, long maxBytes) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        long total = 0;
        try (InputStream source = new DigestInputStream(in, digest); OutputStream out = Files.newOutputStream(target)) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = source.read(buffer)) != -1) {
                total += read;
                if (total > maxBytes) {
                    return new CopyResult(total, null, true);
                }
                out.write(buffer, 0, read);
            }
        }
        return new CopyResult(total, HexFormat.of().formatHex(digest.digest()), false);
    }

    private static void deleteQuietly(Path path) {
        if (path != null) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                log.warn("Không xóa được tệp tạm");
            }
        }
    }
}
