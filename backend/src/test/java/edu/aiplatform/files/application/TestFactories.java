package edu.aiplatform.files.application;

import edu.aiplatform.files.infrastructure.FileRefSigner;
import edu.aiplatform.files.port.StoragePort;
import edu.aiplatform.identity.port.AuthorizationPort;
import edu.aiplatform.jobs.port.JobPort;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;

/** Mở constructor package-private của service cho test ở package khác. */
public final class TestFactories {

    private TestFactories() {}

    public static UploadService upload(StoragePort storage, ContentInspector inspector, FileRefSigner signer,
                                       AuthorizationPort authorization, FileAudit audit, JobPort jobs, Path tmp) {
        return new UploadService(storage, inspector, signer, authorization, audit, jobs, 5, tmp, Duration.ofHours(1),
                Clock.systemUTC());
    }

    public static DownloadTokenService tokens(StringRedisTemplate redis, FileAudit audit, Duration ttl) {
        return new DownloadTokenService(redis, audit, ttl, Clock.systemUTC());
    }
}
