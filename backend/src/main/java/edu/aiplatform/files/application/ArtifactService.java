package edu.aiplatform.files.application;

import edu.aiplatform.files.port.*;
import edu.aiplatform.shared.contract.ActorRef;
import java.io.InputStream;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Khung cài đặt của U03: thân hàm tạm trả giá trị rỗng/an toàn hoặc báo chưa cài.
 * Plan U03: Bước 7.
 */
@Service
public class ArtifactService implements ArtifactPort, AvatarPort {

    @Override
    public FileRef store(ActorRef actor, ArtifactPurpose purpose, UploadedFile file) {
        throw new UnsupportedOperationException("Chưa cài: U03");
    }

    @Override
    public StoredFileInfo attach(FileRef fileRef, ActorRef actor, ArtifactPurpose purpose) {
        throw new UnsupportedOperationException("Chưa cài: U03");
    }

    @Override
    public DownloadToken issueDownloadToken(String fileId, UUID accountId) {
        throw new UnsupportedOperationException("Chưa cài: U03");
    }

    @Override
    public InputStream open(String fileId) {
        throw new UnsupportedOperationException("Chưa cài: U03");
    }

    @Override
    public String validateAvatar(FileRef fileRef, ActorRef actor) {
        throw new UnsupportedOperationException("Chưa cài: U03");
    }
}
