package edu.aiplatform.files.application;

import edu.aiplatform.files.domain.FileRefClaims;
import edu.aiplatform.files.domain.StoredFile;
import edu.aiplatform.files.infrastructure.FileRefSigner;
import edu.aiplatform.files.port.ArtifactPort;
import edu.aiplatform.files.port.ArtifactPurpose;
import edu.aiplatform.files.port.AvatarPort;
import edu.aiplatform.files.port.DownloadToken;
import edu.aiplatform.files.port.FileRef;
import edu.aiplatform.files.port.StoragePort;
import edu.aiplatform.files.port.StoredFileInfo;
import edu.aiplatform.files.port.UploadedFile;
import edu.aiplatform.shared.contract.ActorRef;
import edu.aiplatform.shared.web.ApiException;
import java.io.InputStream;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Port tệp cho các unit khác (F2, F4, F5): `attach` đổi `FileRef` lấy `fileId` để unit sở hữu lưu vào dòng của mình;
 * unit sở hữu tự kiểm quyền rồi mới xin token tải (BR-U03-20). Không có hàm xóa (BR-U03-32).
 */
@Service
public class ArtifactService implements ArtifactPort, AvatarPort {

    private final UploadService uploads;
    private final FileRefSigner signer;
    private final StoragePort storage;
    private final DownloadTokenService tokens;

    public ArtifactService(UploadService uploads, FileRefSigner signer, StoragePort storage,
                           DownloadTokenService tokens) {
        this.uploads = uploads;
        this.signer = signer;
        this.storage = storage;
        this.tokens = tokens;
    }

    @Override
    public FileRef store(ActorRef actor, ArtifactPurpose purpose, UploadedFile file) {
        return uploads.upload(actor, purpose, file).fileRef();
    }

    /** Chữ ký đúng, còn hạn, người gắn là người tải lên, đúng `purpose` (BR-U03-30). */
    @Override
    public StoredFileInfo attach(FileRef fileRef, ActorRef actor, ArtifactPurpose purpose) {
        FileRefClaims claims = signer.verify(fileRef)
                .filter(c -> actor != null && c.ownerAccountId().equals(actor.accountId()))
                .filter(c -> c.purpose() == purpose)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "INVALID_FILE_REF",
                        "Tệp tải lên không hợp lệ hoặc đã hết hạn, vui lòng tải lại."));
        return StoredFile.fromMetadata(claims.fileId(), storage.readMetadata(claims.fileId())).info();
    }

    @Override
    public DownloadToken issueDownloadToken(String fileId, UUID accountId) {
        return tokens.issue(fileId, accountId);
    }

    @Override
    public InputStream open(String fileId) {
        return storage.get(fileId);
    }

    /** F5: như `attach` với `purpose = AVATAR`; trả `fileId` để U01 lưu `avatar_file_id`. */
    @Override
    public String validateAvatar(FileRef fileRef, ActorRef actor) {
        return attach(fileRef, actor, ArtifactPurpose.AVATAR).fileId();
    }
}
