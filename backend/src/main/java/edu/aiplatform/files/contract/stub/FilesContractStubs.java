package edu.aiplatform.files.contract.stub;

import edu.aiplatform.files.contract.ArtifactPurpose;
import edu.aiplatform.files.contract.DownloadToken;
import edu.aiplatform.files.contract.FileRef;
import edu.aiplatform.files.contract.FileUpload;
import edu.aiplatform.files.contract.IArtifact;
import edu.aiplatform.files.contract.StoredFileInfo;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.shared.stub.Stubs;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/** Bản tạm của {@code IArtifact} (U03): không lưu tệp thật, trả tham chiếu và thông tin tệp mẫu, nội dung rỗng. */
@AutoConfiguration
@ConditionalOnProperty(name = "platform.contract-stubs.enabled", havingValue = "true", matchIfMissing = true)
public class FilesContractStubs {

    @Bean
    @ConditionalOnMissingBean
    IArtifact artifactStub() {
        return Stubs.announce(IArtifact.class, new IArtifact() {
            @Override
            public FileRef store(Actor actor, ArtifactPurpose purpose, FileUpload file) {
                return new FileRef("stub-" + UUID.randomUUID());
            }

            @Override
            public StoredFileInfo attach(FileRef fileRef, Actor actor, ArtifactPurpose purpose) {
                return new StoredFileInfo("stub-file-" + UUID.randomUUID(), actor.accountId(), purpose, "tep-mau.bin",
                        "application/octet-stream", 0, "");
            }

            @Override
            public DownloadToken issueDownloadToken(String fileId, UUID accountId) {
                return new DownloadToken("/api/v1/files/download/stub", Instant.now().plus(Duration.ofMinutes(5)));
            }

            @Override
            public InputStream open(String fileId) {
                return InputStream.nullInputStream();
            }
        });
    }
}
