package edu.aiplatform.files.contract;

import edu.aiplatform.identity.contract.Actor;
import java.io.InputStream;
import java.util.UUID;

/**
 * Lưu tệp riêng tư trên Drive, gắn tệp vào đối tượng của unit sở hữu, cấp link tải sau khi unit sở hữu
 * đã kiểm quyền, đọc tệp phía server. Dùng bởi U07, U08, U12, U13.
 */
public interface IArtifact {

    FileRef store(Actor actor, ArtifactPurpose purpose, FileUpload file);

    StoredFileInfo attach(FileRef fileRef, Actor actor, ArtifactPurpose purpose);

    DownloadToken issueDownloadToken(String fileId, UUID accountId);

    InputStream open(String fileId);
}
