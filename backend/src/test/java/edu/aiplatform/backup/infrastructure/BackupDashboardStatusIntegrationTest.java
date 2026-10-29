package edu.aiplatform.backup.infrastructure;

import static org.assertj.core.api.Assertions.*;
import com.zaxxer.hikari.HikariDataSource;
import edu.aiplatform.backup.application.BackupStatusService;
import edu.aiplatform.backup.contract.*;
import edu.aiplatform.backup.contract.stub.BackupContractStubs;
import edu.aiplatform.backup.domain.BackupRunRepository;
import edu.aiplatform.shared.error.ApiException;
import java.sql.Connection;
import java.time.*;
import java.util.UUID;
import java.util.concurrent.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

/** Isolated database: production backup migration and real transactional read provider. */
@Testcontainers(disabledWithoutDocker = true)
class BackupDashboardStatusIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    static final Instant TO = Instant.parse("2026-10-22T10:00:00Z");
    static final Instant FROM = Instant.parse("2026-10-15T10:00:00Z");
    static HikariDataSource pool;
    static AnnotationConfigApplicationContext context;
    static JdbcClient jdbc;
    static IBackupDashboardStatus provider;
    static TransactionTemplate tx;

    @Configuration @EnableTransactionManagement
    static class Transactions { }

    @BeforeAll static void start() {
        pool = new HikariDataSource();
        pool.setJdbcUrl(POSTGRES.getJdbcUrl());
        pool.setUsername(POSTGRES.getUsername());
        pool.setPassword(POSTGRES.getPassword());
        pool.setMaximumPoolSize(2);
        Flyway.configure().dataSource(pool).locations("classpath:db/migration/backup").load().migrate();
        jdbc = JdbcClient.create(pool);
        context = new AnnotationConfigApplicationContext();
        context.register(Transactions.class);
        context.registerBean("transactionManager", DataSourceTransactionManager.class, () -> new DataSourceTransactionManager(pool));
        context.registerBean(BackupRunRepository.class, () -> new BackupRunRepository(jdbc));
        context.registerBean(BackupStatusService.class);
        context.register(BackupContractStubs.class);
        context.refresh();
        provider = context.getBean(IBackupDashboardStatus.class);
        tx = new TransactionTemplate(context.getBean(DataSourceTransactionManager.class));
    }

    @AfterAll static void stop() {
        if (context != null) context.close();
        if (pool != null) pool.close();
    }

    @BeforeEach void clear() { jdbc.sql("delete from backup_runs").update(); }

    UUID insert(int day, String state, Instant completed, String error) {
        UUID id = UUID.randomUUID();
        jdbc.sql("""
                insert into backup_runs(id,scheduled_for,state,created_at,started_at,snapshot_at,completed_at,safe_error)
                values(:id,:day,:state,:created,:started,:snapshot,:completed,:error)
                """)
                .param("id", id).param("day", LocalDate.of(2026, 10, day)).param("state", state)
                .param("created", java.sql.Timestamp.from(FROM.minusSeconds(3)))
                .param("started", java.sql.Timestamp.from(FROM.minusSeconds(2)))
                .param("snapshot", java.sql.Timestamp.from(FROM.minusSeconds(1)))
                .param("completed", completed == null ? null : java.sql.Timestamp.from(completed))
                .param("error", error).update();
        return id;
    }

    @Test void countsInclusiveFromExclusiveToAndOnlyFinalFailures() {
        insert(1, "FAILED", FROM, "Quá thời gian xử lý");
        insert(2, "FAILED", FROM.minusNanos(1000), "before");
        insert(3, "FAILED", TO, "at to");
        insert(4, "SUCCESS", FROM.plusSeconds(1), null);
        insert(5, "RUNNING", null, null);
        insert(6, "CREATED", null, null);
        var summary = provider.summarize(FROM, TO);
        assertThat(summary.failedRuns()).isEqualTo(1);
        assertThat(summary.latestSafeError()).isEqualTo("Backup quá thời gian xử lý.");
        assertThat(summary.latestSuccessfulAt()).isEqualTo(FROM.plusSeconds(1));
        assertThat(summary.fromInclusive()).isEqualTo(FROM);
        assertThat(summary.toExclusive()).isEqualTo(TO);
    }

    @Test void historicalPurgedSuccessAndRealStartSurviveNewerFailures() {
        UUID success = insert(1, "SUCCESS", FROM.minusSeconds(1), null);
        jdbc.sql("update backup_runs set purged_at=:at where id=:id")
                .param("at", java.sql.Timestamp.from(FROM)).param("id", success).update();
        insert(2, "FAILED", FROM.plusSeconds(1), "password=secret /archive");
        var summary = provider.summarize(FROM, TO);
        assertThat(summary.latestSuccessfulAt()).isEqualTo(FROM.minusSeconds(1));
        assertThat(summary.latestSafeError()).isEqualTo("Backup thất bại. Vui lòng liên hệ người vận hành.");
        assertThat(provider.latestRun().orElseThrow().startedAt()).isEqualTo(FROM.minusSeconds(2));
        var history = provider.history(org.springframework.data.domain.PageRequest.of(0, 3));
        assertThat(history.getContent().get(1).purgedAt()).isEqualTo(FROM);
        assertThat(context.getBeansOfType(IBackupDashboardStatus.class)).hasSize(1);
    }

    @Test void latestFailureUsesCompletionThenScheduledDateAndHandlesNullError() {
        insert(1, "FAILED", FROM.plusSeconds(2), "Quá thời gian xử lý");
        insert(2, "FAILED", FROM.plusSeconds(1), "older completion");
        assertThat(provider.summarize(FROM, TO).latestSafeError()).isEqualTo("Backup quá thời gian xử lý.");
        insert(3, "FAILED", FROM.plusSeconds(2), null);
        assertThat(provider.summarize(FROM, TO).latestSafeError())
                .isEqualTo("Backup thất bại. Vui lòng liên hệ người vận hành.");
    }

    @Test void emptyDataIsAvailableButMissingTerminalTimeIsUnavailable() {
        assertThat(provider.summarize(FROM, TO)).isEqualTo(new BackupDashboardSummary(FROM, TO, null, 0, null));
        assertThat(provider.latestRun()).isEmpty();
        insert(1, "SUCCESS", null, null);
        assertThatThrownBy(() -> provider.summarize(FROM, TO)).isInstanceOf(ApiException.class);
    }

    @ParameterizedTest @ValueSource(strings = {"summary", "latest", "history"})
    void lockedQueryTimesOutAndReturnsItsConnection(String operation) throws Exception {
        try (Connection blocker = pool.getConnection()) {
            blocker.setAutoCommit(false);
            blocker.createStatement().execute("lock table backup_runs in access exclusive mode");
            long start = System.nanoTime();
            assertThatThrownBy(() -> {
                switch (operation) {
                    case "summary" -> provider.summarize(FROM, TO);
                    case "latest" -> provider.latestRun();
                    case "history" -> provider.history(org.springframework.data.domain.PageRequest.of(0, 3));
                    default -> throw new AssertionError(operation);
                }
            }).isInstanceOf(RuntimeException.class);
            assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(3));
            assertThat(pool.getHikariPoolMXBean().getActiveConnections()).isEqualTo(1);
            blocker.rollback();
        }
        assertThat(provider.summarize(FROM, TO).failedRuns()).isZero();
        assertThat(pool.getHikariPoolMXBean().getActiveConnections()).isZero();
    }

    @Test void concurrentUpdatesNeverMixCountWithSuccessFromAnotherSnapshot() throws Exception {
        UUID id = insert(1, "SUCCESS", FROM.plusSeconds(1), null);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch started = new CountDownLatch(1);
        Future<?> writer = executor.submit(() -> {
            started.countDown();
            for (int i = 0; i < 100; i++) {
                String state = i % 2 == 0 ? "FAILED" : "SUCCESS";
                tx.executeWithoutResult(ignored -> jdbc.sql("update backup_runs set state=:state where id=:id")
                        .param("state", state).param("id", id).update());
            }
        });
        try {
            assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
            for (int i = 0; i < 50; i++) {
                var summary = provider.summarize(FROM, TO);
                if (summary.failedRuns() == 1) assertThat(summary.latestSuccessfulAt()).isNull();
                else {
                    assertThat(summary.failedRuns()).isZero();
                    assertThat(summary.latestSuccessfulAt()).isEqualTo(FROM.plusSeconds(1));
                }
            }
            writer.get(10, TimeUnit.SECONDS);
        } finally { executor.shutdownNow(); }
    }
}
