package edu.aiplatform.files.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.files.contract.ArtifactPurpose;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.shared.error.ApiException;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.utility.DockerImageName;

/** Real Redis TTL and grant redemption; storage is a bounded byte-stream fixture. */
@Testcontainers(disabledWithoutDocker = true)
class DownloadGrantExpiryIntegrationTest {
    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
            .withExposedPorts(6379);

    @Test
    void grantUsesFiveMinuteRedisTtlAndExpiredOrForeignGrantCannotOpenStorage() throws Exception {
        var connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        connectionFactory.start();
        try {
            var redis = new StringRedisTemplate(connectionFactory);
            var tokenService = new DownloadTokenService(redis, new ObjectMapper());
            var storage = mock(IStorage.class);
            var fileAudit = mock(FileAudit.class);
            var downloadService = new DownloadService(tokenService, storage, fileAudit);
            Actor student = new Actor(UUID.randomUUID(), Role.STUDENT);
            String token = tokenService.issue("material-file", student.accountId());
            assertThat(redis.getExpire(DownloadTokenService.key(token))).isBetween(299L, 300L);
            when(storage.metadata("material-file")).thenReturn(Optional.of(new StoredMetadata(
                    student.accountId(), ArtifactPurpose.MATERIAL, "source.pdf", "application/pdf", 1, "sha")));
            when(storage.open("material-file")).thenReturn(new ByteArrayInputStream(new byte[] {1}));
            try (var stream = downloadService.open(student, token).content()) {
                assertThat(stream.read()).isEqualTo(1);
            }
            clearInvocations(storage);
            assertThatThrownBy(() -> downloadService.open(new Actor(UUID.randomUUID(), Role.STUDENT), token))
                    .isInstanceOfSatisfying(ApiException.class,
                            failure -> assertThat(failure.code()).isEqualTo("DOWNLOAD_TOKEN_NOT_FOUND"));
            verifyNoInteractions(storage);
            // Expire only this isolated test key through Redis; do not wait five minutes or alter application TTL.
            assertThat(redis.expire(DownloadTokenService.key(token), Duration.ZERO)).isTrue();
            assertThatThrownBy(() -> downloadService.open(student, token))
                    .isInstanceOfSatisfying(ApiException.class,
                            failure -> assertThat(failure.code()).isEqualTo("DOWNLOAD_TOKEN_NOT_FOUND"));
            verifyNoInteractions(storage);
        } finally {
            connectionFactory.destroy();
        }
    }
}
