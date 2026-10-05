package edu.aiplatform.files.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.aiplatform.audit.port.AuditEvent;
import edu.aiplatform.audit.port.AuditPort;
import edu.aiplatform.audit.port.AuditResult;
import edu.aiplatform.files.FileFixtures.MemoryStorage;
import edu.aiplatform.files.domain.FileRefClaims;
import edu.aiplatform.files.domain.StoredFile;
import edu.aiplatform.files.infrastructure.FileRefSigner;
import edu.aiplatform.files.port.ArtifactPurpose;
import edu.aiplatform.files.port.DownloadToken;
import edu.aiplatform.files.port.FileRef;
import edu.aiplatform.files.port.StoredFileInfo;
import edu.aiplatform.shared.contract.ActorRef;
import edu.aiplatform.shared.contract.Role;
import edu.aiplatform.shared.web.ApiException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;

/** BR-U03-20, 21, 30…32, 40; F2, F5; P4. */
class ArtifactAndTokenTest {

    @TempDir
    Path tmp;

    private final Instant now = Instant.parse("2026-10-05T08:00:00Z");
    private final FileRefSigner signer = new FileRefSigner("secret", Clock.fixed(now, ZoneOffset.UTC));
    private final MemoryStorage storage = new MemoryStorage();
    private final AuditPort auditPort = mock(AuditPort.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final ActorRef owner = new ActorRef(UUID.randomUUID(), Role.TEACHER);
    private DownloadTokenService tokens;
    private ArtifactService artifacts;
    private String fileId;

    @BeforeEach
    void setUp() throws Exception {
        when(redis.opsForValue()).thenReturn(values);
        FileAudit audit = new FileAudit(auditPort);
        tokens = new DownloadTokenService(redis, audit, Duration.ofMinutes(5), Clock.fixed(now, ZoneOffset.UTC));
        artifacts = new ArtifactService(null, signer, storage, tokens);
        fileId = storage.put(Files.write(tmp.resolve("a"), new byte[] {1, 2, 3}),
                StoredFile.metadata(owner.accountId(), ArtifactPurpose.MATERIAL, "application/pdf", "a.pdf", "ab"));
    }

    @Test
    void attachReturnsFileInfoForOwnerAndPurpose() {
        StoredFileInfo info = artifacts.attach(ref(owner.accountId(), ArtifactPurpose.MATERIAL, now.plusSeconds(60)),
                owner, ArtifactPurpose.MATERIAL);
        assertThat(info).isEqualTo(new StoredFileInfo(fileId, "application/pdf", 3, "a.pdf"));
    }

    @Test
    void attachRejectsOtherUserWrongPurposeExpiredOrTamperedRef() {
        FileRef valid = ref(owner.accountId(), ArtifactPurpose.MATERIAL, now.plusSeconds(60));
        ActorRef other = new ActorRef(UUID.randomUUID(), Role.TEACHER);

        assertInvalid(() -> artifacts.attach(valid, other, ArtifactPurpose.MATERIAL));
        assertInvalid(() -> artifacts.attach(valid, owner, ArtifactPurpose.AVATAR));
        assertInvalid(() -> artifacts.attach(ref(owner.accountId(), ArtifactPurpose.MATERIAL, now.minusSeconds(1)),
                owner, ArtifactPurpose.MATERIAL));
        assertInvalid(() -> artifacts.attach(new FileRef(valid.value() + "x"), owner, ArtifactPurpose.MATERIAL));
        assertInvalid(() -> artifacts.attach(new FileRef("garbage"), owner, ArtifactPurpose.MATERIAL));
    }

    @Test
    void validateAvatarRequiresAvatarPurpose() {
        assertInvalid(() -> artifacts.validateAvatar(ref(owner.accountId(), ArtifactPurpose.MATERIAL,
                now.plusSeconds(60)), owner));
        assertThat(artifacts.validateAvatar(ref(owner.accountId(), ArtifactPurpose.AVATAR, now.plusSeconds(60)),
                owner)).isEqualTo(fileId);
    }

    @Test
    void downloadTokenIsHashedInRedisWithFiveMinuteTtl() {
        DownloadToken token = artifacts.issueDownloadToken(fileId, owner.accountId());

        assertThat(token.url()).isEqualTo("/api/v1/files/download/" + token.token());
        assertThat(token.expiresAt()).isEqualTo(now.plus(Duration.ofMinutes(5)));
        assertThat(token.token()).hasSize(43);
        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(values).set(key.capture(), eq(fileId + "|" + owner.accountId()), eq(Duration.ofMinutes(5)));
        assertThat(key.getValue()).startsWith("file:download-token:").doesNotContain(token.token());
    }

    @Test
    void tokenUsedByAnotherAccountIs404AndAudited() {
        when(values.get(any())).thenReturn(fileId + "|" + owner.accountId());
        UUID intruder = UUID.randomUUID();

        assertThat(tokens.resolve("tok", owner.accountId())).isEqualTo(fileId);
        assertThatThrownBy(() -> tokens.resolve("tok", intruder)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.status()).isEqualTo(HttpStatus.NOT_FOUND));
        ArgumentCaptor<AuditEvent> event = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPort).recordDenied(event.capture());
        assertThat(event.getValue().result()).isEqualTo(AuditResult.DENIED);
        assertThat(event.getValue().actorId()).isEqualTo(intruder);
    }

    @Test
    void expiredOrUnknownTokenIs404() {
        when(values.get(any())).thenReturn(null);
        assertThatThrownBy(() -> tokens.resolve("tok", owner.accountId())).isInstanceOf(ApiException.class);
    }

    private FileRef ref(UUID ownerId, ArtifactPurpose purpose, Instant expiresAt) {
        if (purpose == ArtifactPurpose.AVATAR) {
            storage.meta.get(fileId).put("purpose", "AVATAR");
        }
        return signer.sign(new FileRefClaims(fileId, ownerId, purpose, expiresAt));
    }

    private static void assertInvalid(Runnable call) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.code()).isEqualTo("INVALID_FILE_REF"));
    }
}
