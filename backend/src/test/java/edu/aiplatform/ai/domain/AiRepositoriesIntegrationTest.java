package edu.aiplatform.ai.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.ai.contract.AiSource;
import edu.aiplatform.ai.contract.AiTaskState;
import edu.aiplatform.ai.contract.UsageMeasurement;
import edu.aiplatform.ai.domain.AiConnection.EncryptedKey;
import edu.aiplatform.ai.domain.AiConnection.Scope;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * Chạy SQL của các repository U06 trên PostgreSQL đã migrate (biến môi trường {@code U06_TEST_JDBC_URL}, user như
 * {@code app}; {@code U06_TEST_ADMIN_*} để tạo tài khoản mẫu). Không có biến thì bỏ qua; integration test đầy đủ do
 * tester viết.
 */
@EnabledIfEnvironmentVariable(named = "U06_TEST_JDBC_URL", matches = "jdbc:postgresql:.*")
class AiRepositoriesIntegrationTest {

    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    private static JdbcClient jdbc;
    private static UUID account;

    @BeforeAll
    static void setUp() {
        jdbc = JdbcClient.create(new DriverManagerDataSource(System.getenv("U06_TEST_JDBC_URL"),
                System.getenv("U06_TEST_JDBC_USER"), System.getenv("U06_TEST_JDBC_PASSWORD")));
        JdbcClient owner = JdbcClient.create(new DriverManagerDataSource(System.getenv("U06_TEST_ADMIN_JDBC_URL"),
                System.getenv("U06_TEST_ADMIN_USER"), System.getenv("U06_TEST_ADMIN_PASSWORD")));
        account = UUID.randomUUID();
        owner.sql("insert into accounts (id, school_email, role, status, display_name, password_hash) values "
                + "(?, ?, 'TEACHER', 'ACTIVE', 'GV', 'x')").params(account, account + "@fpt.edu.vn").update();
    }

    @Test
    void connectionsAreUniquePerOwnerAndProviderAndKeepCiphertext() {
        AiConnectionRepository repo = new AiConnectionRepository(jdbc);
        AiConnection c = AiConnection.create(UUID.randomUUID(), Scope.PERSONAL, account, AiProvider.CLAUDE,
                "claude-sonnet-5-5", new EncryptedKey(new byte[] {1, 2, 3}, new byte[] {9}, 1), "3kQf", NOW);
        repo.insert(c);
        assertThat(repo.findPersonal(account, AiProvider.CLAUDE)).hasValueSatisfying(found -> {
            assertThat(found.keyCiphertext()).containsExactly(1, 2, 3);
            assertThat(found.keyVersion()).isEqualTo(1);
        });
        assertThatThrownBy(() -> repo.insert(AiConnection.create(UUID.randomUUID(), Scope.PERSONAL, account,
                AiProvider.CLAUDE, "claude-sonnet-5-5", new EncryptedKey(new byte[] {1}, new byte[] {9}, 1), "zzzz", NOW)))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(repo.update(c.revoked(NOW))).isTrue();
        assertThat(repo.update(c.revoked(NOW))).as("version cũ").isFalse();
        assertThat(repo.findPersonal(account, AiProvider.CLAUDE)).isEmpty();
    }

    @Test
    void tasksQuotaUsageAndCredentials() {
        AiTaskRepository tasks = new AiTaskRepository(jdbc);
        AiTask task = new AiTask(UUID.randomUUID(), "ref-" + UUID.randomUUID(), "fp", account, AiSource.SHARED, null,
                AiProvider.GEMINI, "gemini-2.5-flash", "GRADING_PROPOSAL", "evaluation:1", 3, AiTaskState.QUEUED,
                "{\"system\":\"s\",\"user\":\"u\"}", "{\"type\":\"object\"}", null, 500, 700L, LocalDate.now(), null, null,
                null, false, null, NOW, null, 0);
        tasks.insert(task);
        assertThat(tasks.countActive(account)).isEqualTo(1);
        AiTask locked = tasks.lock(task.id()).orElseThrow();
        assertThat(tasks.update(locked.moveTo(AiTaskState.RUNNING, NOW))).isTrue();
        assertThat(tasks.findByRequestRef(task.requestRef())).hasValueSatisfying(t -> {
            assertThat(t.state()).isEqualTo(AiTaskState.RUNNING);
            assertThat(t.inputPayload()).contains("system");
            assertThat(t.quotaDay()).isEqualTo(task.quotaDay());
        });

        QuotaRepository quota = new QuotaRepository(jdbc);
        assertThat(quota.savePolicy(10_000, 100_000, 0, account, NOW) || quota.policy().configured()).isTrue();
        LocalDate day = LocalDate.of(2026, 10, 21);
        String key = account.toString();
        quota.ensureWindow(key, day);
        quota.ensureWindow(key, day);
        quota.adjust(key, day, 700, 0);
        quota.adjust(key, day, -700, 650);
        assertThat(quota.lockWindow(key, day).usedTokens()).isEqualTo(650);
        quota.upsertTeacherQuota(account, 0, account, NOW);
        assertThat(quota.teacherQuota(account)).hasValueSatisfying(t -> assertThat(t.dayTokens()).isZero());
        assertThat(quota.deleteTeacherQuota(account)).isEqualTo(1);

        AiUsageRepository usage = new AiUsageRepository(jdbc);
        usage.insert(new AiUsageRecord(UUID.randomUUID(), account, AiUsageRecord.Source.SHARED, task.id(), null,
                AiProvider.GEMINI, "gemini-2.5-flash", "GRADING_PROPOSAL", null, "READY", 400L, 250L, null, null,
                UsageMeasurement.PROVIDER_MEASURED, NOW));
        usage.insert(new AiUsageRecord(UUID.randomUUID(), account, AiUsageRecord.Source.MCP, null, null, null, null, null,
                "record_grading_proposal", "OK", null, null, null, null, UsageMeasurement.UNAVAILABLE, NOW));
        assertThat(usage.listOwn(account, null, null, null, 0, 10)).hasSize(2);
        assertThat(usage.countOwn(account, AiUsageRecord.Source.MCP, null, null)).isEqualTo(1);
        assertThat(usage.totalsOwn(account, NOW.minusSeconds(60), NOW.plusSeconds(60)).get(AiUsageRecord.Source.SHARED))
                .containsExactly(1, 650);
        assertThat(usage.sharedSummary(NOW.minusSeconds(60), NOW.plusSeconds(60)))
                .anySatisfy(r -> assertThat(r.tokens()).isEqualTo(650));

        McpCredentialRepository credentials = new McpCredentialRepository(jdbc);
        McpCredential credential = new McpCredential(UUID.randomUUID(), account, "Agent", McpCredential.Access.READ_ONLY,
                "h" + UUID.randomUUID().toString().replace("-", ""), "a1b2", NOW.plus(30, ChronoUnit.DAYS), null, null, NOW);
        credentials.insert(credential);
        assertThat(credentials.countActive(account, NOW)).isEqualTo(1);
        assertThat(credentials.findByHash(credential.tokenHash())).isPresent();
        assertThat(credentials.revoke(credential.id(), NOW)).isEqualTo(1);
        assertThat(credentials.revoke(credential.id(), NOW)).isZero();
        assertThat(credentials.listOwn(account)).singleElement()
                .satisfies(c -> assertThat(c.status(NOW)).isEqualTo(McpCredential.Status.REVOKED));
    }
}
