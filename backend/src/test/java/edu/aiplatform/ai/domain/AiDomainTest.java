package edu.aiplatform.ai.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.ai.contract.AiSource;
import edu.aiplatform.ai.contract.AiTaskState;
import edu.aiplatform.ai.contract.ApplyOutcome;
import edu.aiplatform.ai.domain.AiConnection.EncryptedKey;
import edu.aiplatform.ai.domain.AiConnection.Scope;
import edu.aiplatform.ai.domain.AiConnection.Status;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AiDomainTest {

    private static final Instant NOW = Instant.parse("2026-10-21T03:00:00Z");

    @ParameterizedTest
    @CsvSource({"QUEUED,RUNNING,true", "QUEUED,CANCELLED,true", "QUEUED,FAILED,true", "QUEUED,READY,false",
            "RUNNING,READY,true", "RUNNING,FAILED,true", "RUNNING,UNKNOWN,true", "RUNNING,CANCELLED,false",
            "READY,FAILED,false", "UNKNOWN,RUNNING,false"})
    void taskTransitionsFollowTheStateDiagram(AiTaskState from, AiTaskState to, boolean allowed) {
        assertThat(AiTask.canMove(from, to)).isEqualTo(allowed);
    }

    @Test
    void runningRecordsDispatchAndFinishedStatesRecordEnd() {
        AiTask queued = task();
        AiTask running = queued.moveTo(AiTaskState.RUNNING, NOW);
        assertThat(running.dispatchedAt()).isEqualTo(NOW);
        assertThat(running.finishedAt()).isNull();
        AiTask ready = running.moveTo(AiTaskState.READY, NOW.plusSeconds(5));
        assertThat(ready.finished()).isTrue();
        assertThat(ready.finishedAt()).isEqualTo(NOW.plusSeconds(5));
        assertThatThrownBy(() -> ready.moveTo(AiTaskState.FAILED, NOW)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void handledTaskDropsPromptAndResult() {
        AiTask handled = task().moveTo(AiTaskState.RUNNING, NOW).withResult("{\"a\":1}", "req-1")
                .moveTo(AiTaskState.READY, NOW).handled(ApplyOutcome.STALE);
        assertThat(handled.inputPayload()).isNull();
        assertThat(handled.outputSchema()).isNull();
        assertThat(handled.resultPayload()).isNull();
        assertThat(handled.applyResult()).isEqualTo(ApplyOutcome.STALE);
        assertThat(handled.handlerDone()).isTrue();
    }

    @Test
    void errorIsCutTo500Characters() {
        assertThat(task().withError("x".repeat(800)).safeError()).hasSize(500);
    }

    @Test
    void onlyInstitutionalConnectionCanBeDisabledAndRevokeDropsCiphertext() {
        AiConnection personal = AiConnection.create(UUID.randomUUID(), Scope.PERSONAL, UUID.randomUUID(), AiProvider.OPENAI,
                "gpt-4.1-mini", new EncryptedKey(new byte[] {1}, new byte[] {2}, 1), "3kQf", NOW);
        assertThatThrownBy(() -> personal.withStatus(Status.DISABLED, NOW)).isInstanceOf(IllegalStateException.class);
        AiConnection revoked = personal.revoked(NOW);
        assertThat(revoked.status()).isEqualTo(Status.REVOKED);
        assertThat(revoked.keyCiphertext()).isNull();
        assertThat(revoked.keyIv()).isNull();
        assertThat(revoked.keyHint()).isEqualTo("3kQf");

        AiConnection shared = AiConnection.create(UUID.randomUUID(), Scope.INSTITUTIONAL, null, AiProvider.GEMINI,
                "gemini-2.5-flash", new EncryptedKey(new byte[] {1}, new byte[] {2}, 1), "abcd", NOW);
        assertThat(shared.withStatus(Status.DISABLED, NOW).status()).isEqualTo(Status.DISABLED);
    }

    @Test
    void credentialStatusDependsOnRevocationAndExpiry() {
        McpCredential credential = new McpCredential(UUID.randomUUID(), UUID.randomUUID(), "Claude Desktop",
                McpCredential.Access.READ_ONLY, "hash", "a1b2", NOW.plus(Duration.ofDays(30)), null, null, NOW);
        assertThat(credential.status(NOW)).isEqualTo(McpCredential.Status.ACTIVE);
        assertThat(credential.status(NOW.plus(Duration.ofDays(31)))).isEqualTo(McpCredential.Status.EXPIRED);
        McpCredential revoked = new McpCredential(credential.id(), credential.accountId(), credential.label(),
                credential.access(), credential.tokenHash(), credential.tokenHint(), credential.expiresAt(), NOW, null, NOW);
        assertThat(revoked.status(NOW)).isEqualTo(McpCredential.Status.REVOKED);
    }

    @Test
    void quotaPolicyNeedsBothLimits() {
        assertThat(QuotaPolicy.empty().configured()).isFalse();
        assertThat(new QuotaPolicy(1000L, null, 1, null, NOW).configured()).isFalse();
        assertThat(new QuotaPolicy(1000L, 50000L, 1, null, NOW).configured()).isTrue();
    }

    private static AiTask task() {
        return new AiTask(UUID.randomUUID(), "ref-1", "fp", UUID.randomUUID(), AiSource.SHARED, UUID.randomUUID(),
                AiProvider.GEMINI, "gemini-2.5-flash", "GRADING_PROPOSAL", "evaluation:1", 1, AiTaskState.QUEUED,
                "{\"system\":\"s\",\"user\":\"u\"}", "{}", null, 1000, 1200L, null, null, null, null, false, null, NOW,
                null, 0);
    }
}
