package edu.aiplatform.files.port;

import edu.aiplatform.shared.contract.ActorRef;
import java.io.InputStream;
import java.util.UUID;

/** U03 cung cấp; unit sở hữu tự kiểm quyền trước khi cấp token (BR-U03-20). */
public interface ArtifactPort {
    FileRef store(ActorRef actor, ArtifactPurpose purpose, UploadedFile file);
    StoredFileInfo attach(FileRef fileRef, ActorRef actor, ArtifactPurpose purpose);
    DownloadToken issueDownloadToken(String fileId, UUID accountId);
    InputStream open(String fileId);
}
