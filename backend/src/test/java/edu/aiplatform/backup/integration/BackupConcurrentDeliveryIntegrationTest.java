package edu.aiplatform.backup.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.zaxxer.hikari.HikariDataSource;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.backup.config.*;
import edu.aiplatform.backup.service.*;
import edu.aiplatform.backup.contract.BackupState;
import edu.aiplatform.backup.repository.BackupRunRepository;
import edu.aiplatform.jobs.contract.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.time.*;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

/** Two proxied handlers and real row transitions, using the production backup migration. */
@Testcontainers(disabledWithoutDocker = true)
class BackupConcurrentDeliveryIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    @Configuration @EnableTransactionManagement(proxyTargetClass = true) static class Transactions { }
    @TempDir Path dir;

    @Test
    void concurrentDeliveryRunsOnlyOneDumpAndPreservesItsSuccessfulArchive() throws Exception {
        try (HikariDataSource pool = new HikariDataSource();
             AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            pool.setJdbcUrl(POSTGRES.getJdbcUrl());
            pool.setUsername(POSTGRES.getUsername());
            pool.setPassword(POSTGRES.getPassword());
            pool.setMaximumPoolSize(3);
            Flyway.configure().dataSource(pool).locations("classpath:db/migration/backup").load().migrate();
            BackupRunRepository runs = new BackupRunRepository(JdbcClient.create(pool));
            Instant now = Instant.parse("2026-10-30T00:00:00Z");
            var runId = runs.createIfAbsent(LocalDate.of(2026, 10, 30), now).orElseThrow();
            JobMessage message = new JobMessage(1, JobTypes.BACKUP_RUN, "backup:2026-10-30",
                    Map.of("runId", runId.toString()), 0, "c");
            CountDownLatch firstDump = new CountDownLatch(1);
            CountDownLatch duplicateDump = new CountDownLatch(1);
            CountDownLatch releaseDump = new CountDownLatch(1);
            AtomicInteger dumps = new AtomicInteger();
            IDumpRunner dump = target -> {
                int delivery = dumps.incrementAndGet();
                if (delivery == 1) firstDump.countDown(); else duplicateDump.countDown();
                try {
                    if (!releaseDump.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("test timed out");
                    Files.writeString(target, "database snapshot");
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            };
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            String key = Base64.getEncoder().encodeToString(generator.generateKeyPair().getPublic().getEncoded());
            BackupProperties props = new BackupProperties(null, key, "localhost", 5432, "platform", "reader",
                    "", dir.resolve("tmp").toString(), null);
            LocalBackupStore store = new LocalBackupStore(dir.resolve("archives"));
            Instant oldSnapshot = now.minus(Duration.ofDays(10));
            var oldRun = runs.createIfAbsent(LocalDate.of(2026, 10, 20), oldSnapshot).orElseThrow();
            runs.markRunning(oldRun, oldSnapshot);
            Path oldArchive = Files.writeString(dir.resolve("old.alapbk"), "old encrypted archive");
            store.put(oldRun, oldArchive);
            runs.markSuccess(oldRun, oldSnapshot, "test", "test", oldRun.toString(), "a".repeat(64),
                    Files.size(oldArchive), oldSnapshot);
            context.register(Transactions.class);
            context.registerBean("transactionManager", DataSourceTransactionManager.class,
                    () -> new DataSourceTransactionManager(pool));
            context.registerBean(BackupRetentionService.class, () -> new BackupRetentionService(runs, store));
            context.registerBean(BackupJobHandler.class, () -> new BackupJobHandler(runs, dump, store,
                    context.getBean(BackupRetentionService.class), mock(IAudit.class), mock(IEventPublisher.class),
                    mock(IJob.class), props, Clock.fixed(now, ZoneOffset.UTC)));
            context.refresh();
            BackupJobHandler handler = context.getBean(BackupJobHandler.class);
            ExecutorService workers = Executors.newFixedThreadPool(2);
            try {
                Future<?> first = workers.submit(() -> handler.handle(message));
                assertThat(firstDump.await(3, TimeUnit.SECONDS)).isTrue();
                Future<?> second = workers.submit(() -> handler.handle(message));
                boolean duplicated = duplicateDump.await(1, TimeUnit.SECONDS);
                releaseDump.countDown();
                assertThat(duplicated).as("only the owner may dump while a delivery is running").isFalse();
                first.get(5, TimeUnit.SECONDS);
                second.get(5, TimeUnit.SECONDS);
                assertThat(dumps.get()).isEqualTo(1);
                assertThat(runs.find(runId).orElseThrow().state()).isEqualTo(BackupState.SUCCESS);
                assertThat(dir.resolve("archives").resolve(runId.toString()).resolve("archive.alapbk"))
                        .isRegularFile();
                assertThat(runs.find(oldRun).orElseThrow().purgedAt()).isEqualTo(now);
                assertThat(dir.resolve("archives").resolve(oldRun.toString())).doesNotExist();
            } finally {
                releaseDump.countDown();
                workers.shutdownNow();
                workers.awaitTermination(5, TimeUnit.SECONDS);
            }
        }
    }
}
