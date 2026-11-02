package edu.aiplatform.ai.model;

import edu.aiplatform.ai.repository.AiConnectionRepository;
import edu.aiplatform.ai.repository.AiTaskRepository;
import edu.aiplatform.ai.repository.AiUsageRepository;
import edu.aiplatform.ai.repository.McpCredentialRepository;
import edu.aiplatform.ai.repository.QuotaRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zaxxer.hikari.HikariDataSource;
import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.ai.contract.AiSource;
import edu.aiplatform.ai.contract.AiTaskState;
import edu.aiplatform.ai.contract.UsageMeasurement;
import edu.aiplatform.ai.model.AiConnection.EncryptedKey;
import edu.aiplatform.ai.model.AiConnection.Scope;
import edu.aiplatform.ai.model.AiConnection.Status;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Production U06 SQL and constraints, with real Flyway migrations and the restricted app database role. */
@Testcontainers(disabledWithoutDocker = true)
class AiRepositoriesIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-10-30T00:00:00Z");
    private static final String APP_PASSWORD = UUID.randomUUID().toString();
    private static final String MIGRATOR_PASSWORD = UUID.randomUUID().toString();

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withEnv("POSTGRES_APP_PASSWORD", APP_PASSWORD)
            .withEnv("POSTGRES_MIGRATOR_PASSWORD", MIGRATOR_PASSWORD)
            .withCopyToContainer(rolesScript(), "/docker-entrypoint-initdb.d/01-roles.sh");

    private static HikariDataSource appDataSource;
    private static HikariDataSource adminDataSource;
    private static JdbcClient jdbc;
    private static JdbcClient admin;
    private UUID account;

    @BeforeAll
    static void setUp() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), "migrator", MIGRATOR_PASSWORD)
                .locations("classpath:db/migration").load().migrate();
        appDataSource = dataSource("app", APP_PASSWORD);
        adminDataSource = dataSource(POSTGRES.getUsername(), POSTGRES.getPassword());
        jdbc = JdbcClient.create(appDataSource);
        admin = JdbcClient.create(adminDataSource);
    }

    @AfterAll
    static void close() {
        if (appDataSource != null) appDataSource.close();
        if (adminDataSource != null) adminDataSource.close();
    }

    @BeforeEach
    void seed() {
        admin.sql("truncate accounts, ai_quota_windows cascade").update();
        account = UUID.randomUUID();
        admin.sql("insert into accounts (id, school_email, role, status, display_name, password_hash) values "
                + "(?, ?, 'TEACHER', 'ACTIVE', 'GV', 'x')").params(account, account + "@fpt.edu.vn").update();
    }

    private static Transferable rolesScript() {
        try {
            String script = Files.readString(Path.of("..", "infra", "postgres", "init", "01-roles.sh"));
            return Transferable.of(script.replace("\r", ""), 0755);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static HikariDataSource dataSource(String user, String password) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(POSTGRES.getJdbcUrl());
        dataSource.setUsername(user);
        dataSource.setPassword(password);
        dataSource.setMaximumPoolSize(3);
        return dataSource;
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
        AiConnection replacement = AiConnection.create(UUID.randomUUID(), Scope.PERSONAL, account, AiProvider.CLAUDE,
                "claude-sonnet-5-5", new EncryptedKey(new byte[] {4}, new byte[] {8}, 2), "next", NOW);
        repo.insert(replacement);
        assertThat(repo.findPersonal(account, AiProvider.CLAUDE)).hasValueSatisfying(found ->
                assertThat(found.id()).isEqualTo(replacement.id()));
    }

    @Test
    void multipleInstitutionalKeysPreferActiveThenCreationTimeAndId() {
        AiConnectionRepository repo = new AiConnectionRepository(jdbc);
        AiConnection disabledOld = institutional(1, AiProvider.CLAUDE, NOW.minusSeconds(60))
                .withStatus(Status.DISABLED, NOW);
        AiConnection revokedOld = institutional(2, AiProvider.CLAUDE, NOW.minusSeconds(120)).revoked(NOW);
        AiConnection earlierId = institutional(3, AiProvider.CLAUDE, NOW);
        AiConnection laterId = institutional(4, AiProvider.CLAUDE, NOW);
        AiConnection newest = institutional(5, AiProvider.CLAUDE, NOW.plusSeconds(60));
        AiConnection otherProvider = institutional(6, AiProvider.GEMINI, NOW.minusSeconds(180));
        // Deliberately insert out of order: result order must come from SQL, including the UUID tie-break.
        repo.insert(newest);
        repo.insert(laterId);
        repo.insert(disabledOld);
        repo.insert(otherProvider);
        repo.insert(earlierId);
        repo.insert(revokedOld);

        assertThat(repo.findInstitutional(AiProvider.CLAUDE)).hasValueSatisfying(found ->
                assertThat(found.id()).isEqualTo(earlierId.id()));
        assertThat(repo.findInstitutional(AiProvider.GEMINI)).hasValueSatisfying(found ->
                assertThat(found.id()).isEqualTo(otherProvider.id()));
        assertThat(repo.listInstitutional()).extracting(AiConnection::id)
                .containsExactly(disabledOld.id(), earlierId.id(), laterId.id(), newest.id(), otherProvider.id());

        assertThat(repo.update(earlierId.withStatus(Status.DISABLED, NOW))).isTrue();
        assertThat(repo.findInstitutional(AiProvider.CLAUDE)).hasValueSatisfying(found ->
                assertThat(found.id()).isEqualTo(laterId.id()));
        assertThat(repo.update(laterId.withStatus(Status.DISABLED, NOW))).isTrue();
        assertThat(repo.findInstitutional(AiProvider.CLAUDE)).hasValueSatisfying(found ->
                assertThat(found.id()).isEqualTo(newest.id()));
        assertThat(repo.update(newest.withStatus(Status.DISABLED, NOW.plusSeconds(60)))).isTrue();
        assertThat(repo.findInstitutional(AiProvider.CLAUDE)).hasValueSatisfying(found ->
                assertThat(found.id()).isEqualTo(disabledOld.id()));
    }

    private AiConnection institutional(long id, AiProvider provider, Instant createdAt) {
        return AiConnection.create(new UUID(0, id), Scope.INSTITUTIONAL, null, provider, "test-model",
                new EncryptedKey(new byte[] {1, 2, 3}, new byte[] {9}, 1), "hint", createdAt);
    }

    @Test
    void tasksQuotaUsageAndCredentials() {
        AiTaskRepository tasks = new AiTaskRepository(jdbc);
        AiTask task = new AiTask(UUID.randomUUID(), "ref-" + UUID.randomUUID(), "fp", account, AiSource.SHARED, null,
                AiProvider.GEMINI, "gemini-2.5-flash", "GRADING_PROPOSAL", "evaluation:1", 3, AiTaskState.QUEUED,
                "{\"system\":\"s\",\"user\":\"u\"}", "{\"type\":\"object\"}", null, 500, 700L, LocalDate.of(2026, 10, 30), null, null,
                null, false, null, NOW, null, 0);
        tasks.insert(task);
        tasks.lockActor(account);
        assertThat(tasks.countActive(account)).isEqualTo(1);
        AiTask locked = tasks.lock(task.id()).orElseThrow();
        assertThat(tasks.update(locked.moveTo(AiTaskState.RUNNING, NOW))).isTrue();
        assertThat(tasks.findByRequestRef(task.requestRef())).hasValueSatisfying(t -> {
            assertThat(t.state()).isEqualTo(AiTaskState.RUNNING);
            assertThat(t.inputPayload()).contains("system");
            assertThat(t.quotaDay()).isEqualTo(task.quotaDay());
        });

        QuotaRepository quota = new QuotaRepository(jdbc);
        assertThat(quota.savePolicy(10_000, 100_000, 0, account, NOW)).isTrue();
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

    @Test
    void legacyCredentialsRemainHashAuthenticatableWithoutRevealableCiphertext() {
        McpCredentialRepository credentials = new McpCredentialRepository(jdbc);
        McpCredential legacy = credential(UUID.randomUUID(), "a".repeat(64), NOW.plus(90, ChronoUnit.DAYS));
        credentials.insert(legacy);

        assertThat(credentials.findByHash(legacy.tokenHash())).hasValue(legacy);
        assertThat(credentials.findSecret(legacy.id())).isEmpty();
        assertThat(credentials.revealableIds(account)).isEmpty();
        credentials.saveSecret(legacy.id(), new EncryptedKey(new byte[] {4, 5, 6}, new byte[] {7, 8}, 2));
        assertThat(credentials.findSecret(legacy.id())).hasValueSatisfying(secret -> {
            assertThat(secret.ciphertext()).containsExactly(4, 5, 6);
            assertThat(secret.iv()).containsExactly(7, 8);
            assertThat(secret.version()).isEqualTo(2);
        });
        assertThat(credentials.revealableIds(account)).containsExactly(legacy.id());
        assertThat(credentials.findByHash(legacy.tokenHash())).hasValue(legacy);
    }

    @Test
    void credentialHashUniquenessAndNinetyDayExpiryRemainDatabaseConstraints() {
        McpCredentialRepository credentials = new McpCredentialRepository(jdbc);
        credentials.insert(credential(UUID.randomUUID(), "a".repeat(64), NOW.plus(90, ChronoUnit.DAYS)));

        assertThatThrownBy(() -> credentials.insert(credential(UUID.randomUUID(), "a".repeat(64),
                NOW.plus(30, ChronoUnit.DAYS)))).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> credentials.insert(credential(UUID.randomUUID(), "b".repeat(64),
                NOW.plus(91, ChronoUnit.DAYS)))).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(credentials.countActive(account, NOW)).isEqualTo(1);
    }

    private McpCredential credential(UUID id, String hash, Instant expiresAt) {
        return new McpCredential(id, account, "Agent", McpCredential.Access.READ_ONLY, hash, "hint", expiresAt,
                null, null, NOW);
    }
}
