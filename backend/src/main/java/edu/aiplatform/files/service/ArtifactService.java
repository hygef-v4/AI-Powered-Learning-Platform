package edu.aiplatform.files.service;

import edu.aiplatform.files.contract.ArtifactPurpose;
import edu.aiplatform.files.contract.DownloadToken;
import edu.aiplatform.files.contract.FileRef;
import edu.aiplatform.files.contract.FileUpload;
import edu.aiplatform.files.contract.IArtifact;
import edu.aiplatform.files.contract.StoredFileInfo;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.shared.error.ApiException;
import java.io.InputStream;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Cài {@link IArtifact} cho U07, U08, U12, U13 (F1–F4). */
@Service
public class ArtifactService implements IArtifact {

    static final String DOWNLOAD_PATH = "/api/v1/files/download/";

    private final UploadService uploads;
    private final FileRefSigner signer;
    private final DownloadTokenService tokens;
    private final IStorage storage;
    private final Clock clock;

    public ArtifactService(UploadService uploads, FileRefSigner signer, DownloadTokenService tokens, IStorage storage,
                           Clock clock) {
        this.uploads = uploads;
        this.signer = signer;
        this.tokens = tokens;
        this.storage = storage;
        this.clock = clock;
    }

    @Override
    public FileRef store(Actor actor, ArtifactPurpose purpose, FileUpload file) {
        return uploads.upload(actor, purpose, file).fileRef();
    }

    /** Kiểm chữ ký, hạn, người gắn là người tải lên và đúng {@code purpose} (BR-U03-30). */
    @Override
    public StoredFileInfo attach(FileRef fileRef, Actor actor, ArtifactPurpose purpose) {
        FileRefSigner.Payload payload = signer.verify(fileRef)
                .orElseThrow(() -> ApiException.badRequest("FILE_REF_INVALID", "Tham chiếu tệp không hợp lệ"));
        if (payload.expiresAt().isBefore(clock.instant())) {
            throw ApiException.badRequest("FILE_REF_EXPIRED", "Tham chiếu tệp đã hết hạn, tải tệp lên lại");
        }
        if (!payload.ownerAccountId().equals(actor.accountId()) || payload.purpose() != purpose) {
            throw ApiException.forbidden("FILE_REF_NOT_OWNED");
        }
        return new StoredFileInfo(payload.fileId(), payload.ownerAccountId(), payload.purpose(),
                payload.originalFileName(), payload.mediaType(), payload.byteSize(), payload.sha256());
    }

    /** Unit sở hữu gọi sau khi đã tự kiểm quyền (BR-U03-20). */
    @Override
    public DownloadToken issueDownloadToken(String fileId, UUID accountId) {
        String token = tokens.issue(fileId, accountId);
        return new DownloadToken(DOWNLOAD_PATH + token, clock.instant().plus(DownloadTokenService.TTL));
    }

    @Override
    public InputStream open(String fileId) {
        return storage.open(fileId);
    }
}
