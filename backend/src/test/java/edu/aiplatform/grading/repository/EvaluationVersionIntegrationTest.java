package edu.aiplatform.grading.repository;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.grading.model.Evaluation;
import edu.aiplatform.grading.model.GradingFeedback;
import edu.aiplatform.grading.model.GradingProposal;
import edu.aiplatform.grading.repository.IEvaluationRepository;
import edu.aiplatform.grading.repository.IGradingProposalRepository;
import edu.aiplatform.shared.error.ApiException;
import org.flywaydb.core.Flyway;
import org.testcontainers.images.builder.Transferable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.UncheckedIOException;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

/** Real owner migrations/app grants and committed versions, in a disposable database only. */
@Testcontainers(disabledWithoutDocker = true)
class EvaluationVersionIntegrationTest {
    static final String APP_PASSWORD = UUID.randomUUID().toString();
    static final String MIGRATOR_PASSWORD = UUID.randomUUID().toString();
    @Container
    static final PostgreSQLContainer<?> DB = new PostgreSQLContainer<>("postgres:16-alpine")
            .withEnv("POSTGRES_APP_PASSWORD", APP_PASSWORD)
            .withEnv("POSTGRES_MIGRATOR_PASSWORD", MIGRATOR_PASSWORD)
            .withCopyToContainer(rolesScript(), "/docker-entrypoint-initdb.d/01-roles.sh");
    static JdbcTemplate owner;
    static AnnotationConfigApplicationContext context;
    static IEvaluationRepository evaluationRepository;
    static IGradingProposalRepository gradingProposalRepository;
    static TransactionTemplate transactionTemplate;
    static final Instant NOW = Instant.parse("2026-10-23T11:00:00Z");

    @Configuration
    @EnableTransactionManagement
    @Import({ EvaluationRepositoryAdapter.class, GradingProposalRepositoryAdapter.class })
    static class Config {
        @Bean
        DataSource dataSource() {
            Flyway.configure().dataSource(DB.getJdbcUrl(), "migrator", MIGRATOR_PASSWORD)
                    .locations("classpath:db/migration").load().migrate();
            owner = new JdbcTemplate(new DriverManagerDataSource(DB.getJdbcUrl(), DB.getUsername(),
                    DB.getPassword()));
            return new DriverManagerDataSource(DB.getJdbcUrl(), "app", APP_PASSWORD);
        }

        @Bean
        NamedParameterJdbcTemplate namedTemplate(DataSource source) {
            return new NamedParameterJdbcTemplate(source);
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource source) {
            return new DataSourceTransactionManager(source);
        }
    }

    @BeforeAll
    static void setup() {
        context = new AnnotationConfigApplicationContext(Config.class);
        evaluationRepository = context.getBean(IEvaluationRepository.class);
        gradingProposalRepository = context.getBean(IGradingProposalRepository.class);
        transactionTemplate = new TransactionTemplate(
                context.getBean(PlatformTransactionManager.class));
        transactionTemplate.setTimeout(10);
    }

    @AfterAll
    static void close() {
        if (context != null)
            context.close();
    }

    Evaluation seed() {
        UUID account = UUID.randomUUID(), subject = UUID.randomUUID(), course = UUID.randomUUID();
        UUID assignment = UUID.randomUUID(), attempt = UUID.randomUUID();
        owner.update(
                "insert into accounts (id,school_email,role,status,display_name,password_hash) values (?,?,'STUDENT','ACTIVE','Fixture','fixture-only')",
                account, account + "@example.test");
        owner.update(
                "insert into subjects (id,code,name,status,created_at,updated_at) values (?,?,'Fixture','ACTIVE',now(),now())",
                subject, subject.toString().substring(0, 8).toUpperCase());
        owner.update(
                "insert into course_classes (id,subject_id,code,name,status,created_at,updated_at) values (?,?,'C','Fixture','OPEN',now(),now())",
                course, subject);
        owner.update(
                """
                        insert into assignments (id,scope,class_id,subject_id,origin,submission_mode,grading_mode,
                            title,skeleton,parts,created_by,created_at,updated_at)
                        values (?,'CLASS',?,?,'MANUAL','INDIVIDUAL','GRADED','Fixture','{}','[]',?,now(),now())
                        """,
                assignment, course, subject, account);
        owner.update(
                """
                        insert into assignment_attempts (id,assignment_id,class_id,account_id,attempt_no,snapshot,
                            content,started_at,deadline_at) values (?,?,?,?,1,'{}','{}',now(),now()+interval '1 hour')
                        """,
                attempt, assignment, course, account);
        var e = Evaluation.createPendingEvaluation(attempt, assignment, course, account, "hash",
                BigDecimal.TEN, "[]", NOW);
        return transactionTemplate.execute(status -> evaluationRepository.save(e));
    }

    Evaluation read(UUID id) {
        return transactionTemplate
                .execute(status -> evaluationRepository.findById(id).orElseThrow());
    }

    private static Transferable rolesScript() {
        try {
            return Transferable
                    .of(Files.readString(Path.of("..", "infra", "postgres", "init", "01-roles.sh"))
                            .replace("\r", ""), 0755);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    @Test
    void lookupScopeAndMutationWithoutSaveDoNotWrite() {
        var created = seed();
        var other = seed();
        assertThat(evaluationRepository.findByAttemptId(created.attemptId)).get()
                .extracting(e -> e.id).isEqualTo(created.id);
        assertThat(evaluationRepository.findForAssignmentAndClass(created.assignmentId,
                created.classId)).extracting(e -> e.id).containsExactly(created.id);
        assertThat(evaluationRepository.findForClass(other.classId)).extracting(e -> e.id)
                .containsExactly(other.id);
        transactionTemplate.executeWithoutResult(status -> {
            var value = evaluationRepository.findById(created.id).orElseThrow();
            value.feedback = "not saved";
        });
        assertThat(read(created.id).feedback).isNull();
        assertThat(read(created.id).version).isEqualTo(created.version);
    }

    @Test
    void callerRollbackRestoresDocumentAndVersion() {
        var created = seed();
        transactionTemplate.executeWithoutResult(status -> {
            var value = evaluationRepository.lockById(created.id).orElseThrow();
            value.feedback = "rolled back";
            evaluationRepository.save(value);
            status.setRollbackOnly();
        });
        assertThat(read(created.id).feedback).isNull();
        assertThat(read(created.id).version).isEqualTo(created.version);
    }

    @Test
    void concurrentWritersCannotBothSaveOneExpectedVersion() throws Exception {
        var created = seed();
        var ready = new java.util.concurrent.CountDownLatch(2);
        var start = new java.util.concurrent.CountDownLatch(1);
        var workers = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Callable<Boolean> write = () -> {
                try {
                    return transactionTemplate.execute(status -> {
                        var value = evaluationRepository.findById(created.id).orElseThrow();
                        ready.countDown();
                        await(start);
                        value.feedback = UUID.randomUUID().toString();
                        evaluationRepository.save(value);
                        return true;
                    });
                } catch (ApiException
                        | org.springframework.dao.OptimisticLockingFailureException conflict) {
                    return false;
                }
            };
            var first = workers.submit(write);
            var second = workers.submit(write);
            assertThat(ready.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(List.of(first.get(10, java.util.concurrent.TimeUnit.SECONDS),
                    second.get(10, java.util.concurrent.TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
            assertThat(read(created.id).version).isEqualTo(created.version + 1);
        } finally {
            start.countDown();
            workers.shutdownNow();
        }
    }

    @Test
    void rowAndAdvisoryLocksAreHeldUntilCallerCommit() throws Exception {
        var created = seed();
        for (boolean advisory : new boolean[] { false, true }) {
            var held = new java.util.concurrent.CountDownLatch(1);
            var release = new java.util.concurrent.CountDownLatch(1);
            var acquired = new java.util.concurrent.atomic.AtomicBoolean();
            var workers = java.util.concurrent.Executors.newFixedThreadPool(2);
            Runnable lock = () -> {
                if (advisory)
                    evaluationRepository.lockIdentity(created.classId, created.assignmentId,
                            created.accountId);
                else
                    evaluationRepository.lockById(created.id).orElseThrow();
            };
            try {
                var first = workers
                        .submit(() -> transactionTemplate.executeWithoutResult(status -> {
                            lock.run();
                            held.countDown();
                            await(release);
                        }));
                assertThat(held.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
                var second = workers
                        .submit(() -> transactionTemplate.executeWithoutResult(status -> {
                            lock.run();
                            acquired.set(true);
                        }));
                long deadline = System.nanoTime() + java.time.Duration.ofSeconds(3).toNanos();
                boolean waiting = false;
                while (System.nanoTime() < deadline && !waiting) {
                    waiting = Boolean.TRUE.equals(owner.queryForObject(
                            """
                                    select exists (select 1 from pg_stat_activity where datname=current_database()
                                        and usename='app' and wait_event_type='Lock')
                                    """,
                            Boolean.class));
                    if (!waiting)
                        java.util.concurrent.locks.LockSupport.parkNanos(2_000_000);
                }
                assertThat(waiting).as("actual PostgreSQL lock wait").isTrue();
                assertThat(acquired).isFalse();
                release.countDown();
                first.get(10, java.util.concurrent.TimeUnit.SECONDS);
                second.get(10, java.util.concurrent.TimeUnit.SECONDS);
                assertThat(acquired).isTrue();
            } finally {
                release.countDown();
                workers.shutdownNow();
            }
        }
    }

    private static void await(java.util.concurrent.CountDownLatch latch) {
        try {
            if (!latch.await(10, java.util.concurrent.TimeUnit.SECONDS))
                throw new AssertionError("Latch timed out");
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(failure);
        }
    }

    @Test
    void readingJsonDoesNotWriteOrInvalidateVersion() {
        var created = seed();
        assertThat(new JdbcTemplate(context.getBean(DataSource.class)).queryForObject(
                "select jsonb_typeof(grading_document) from evaluations where id=?", String.class,
                created.id)).isEqualTo("object");
        assertThat(read(created.id).version).isEqualTo(created.version);
        assertThat(read(created.id).version).isEqualTo(created.version);
    }

    @Test
    void pollingProposalsInsideTransactionDoesNotDirtyWriteOrIncrementProposalVersion()
            throws Exception {
        var created = seed();
        var jdbc = new JdbcTemplate(context.getBean(DataSource.class));
        var json = new ObjectMapper().findAndRegisterModules();

        var p = new GradingProposal();
        p.id = UUID.randomUUID();
        p.evaluationId = created.id;
        p.requesterId = UUID.randomUUID();
        p.versionNo = 1;
        p.requestRef = "req-" + p.id;
        p.submissionHash = created.submissionHash;
        p.rubricFingerprint = created.rubricSnapshot;
        p.status = "PENDING";
        p.createdAt = NOW;
        transactionTemplate.execute(status -> gradingProposalRepository.save(p));

        assertThat(jdbc.queryForObject(
                "select jsonb_typeof(proposal_document) from grading_proposals where id=?",
                String.class, p.id)).isEqualTo("object");
        Long initialVersion = jdbc.queryForObject(
                "select version from grading_proposals where id=?", Long.class, p.id);
        assertThat(initialVersion).isZero();

        // Simulate repeated polling transactions (lock evaluation + read proposals).
        for (int i = 0; i < 5; i++) {
            var listed = transactionTemplate.execute(status -> {
                evaluationRepository.lockById(created.id).orElseThrow();
                return gradingProposalRepository.findForEvaluation(created.id);
            });
            assertThat(listed).singleElement()
                    .satisfies(found -> assertThat(found.status).isEqualTo("PENDING"));
        }
        Long versionAfterPolls = jdbc.queryForObject(
                "select version from grading_proposals where id=?", Long.class, p.id);
        assertThat(versionAfterPolls).as("read polls must not dirty-write grading_proposals")
                .isEqualTo(initialVersion);

        // Callback delivery updates proposal to READY in its transaction cleanly.
        UUID taskId = UUID.randomUUID();
        var payload = json
                .readTree("{\"checks\":{},\"score\":8.5,\"feedback\":\"Tốt\",\"sections\":[]}");
        var delivered = transactionTemplate.execute(status -> {
            evaluationRepository.lockById(created.id).orElseThrow();
            var match = gradingProposalRepository.findForEvaluation(created.id).get(0);
            match.taskId = taskId;
            match.status = "READY";
            match.proposal = payload;
            match.completedAt = NOW.plusSeconds(3);
            return gradingProposalRepository.save(match);
        });
        assertThat(delivered.status).isEqualTo("READY");
        assertThat(delivered.taskId).isEqualTo(taskId);
        Long versionAfterDelivery = jdbc.queryForObject(
                "select version from grading_proposals where id=?", Long.class, p.id);
        assertThat(versionAfterDelivery).isEqualTo(initialVersion + 1);
    }

    @Test
    void proposalsPreserveLookupOrderingSnapshotsAndRollback() {
        var created = seed();
        var first = proposal(created, 1);
        var second = proposal(created, 2);
        transactionTemplate.executeWithoutResult(status -> {
            gradingProposalRepository.save(first);
            gradingProposalRepository.save(second);
        });
        assertThat(gradingProposalRepository.findEvaluationId(first.id)).contains(created.id);
        assertThat(gradingProposalRepository.findByRequestRef(first.requestRef)).get()
                .extracting(p -> p.id).isEqualTo(first.id);
        assertThat(gradingProposalRepository.findForEvaluation(created.id))
                .extracting(p -> p.versionNo).containsExactly(2L, 1L);
        var snapshot = gradingProposalRepository.findById(first.id).orElseThrow();
        snapshot.status = "FAILED";
        assertThat(gradingProposalRepository.findById(first.id).orElseThrow().status)
                .isEqualTo("PENDING");
        transactionTemplate.executeWithoutResult(status -> {
            gradingProposalRepository.save(snapshot);
            status.setRollbackOnly();
        });
        assertThat(gradingProposalRepository.findById(first.id).orElseThrow().status)
                .isEqualTo("PENDING");
        var duplicate = proposal(created, 3);
        duplicate.requestRef = first.requestRef;
        assertThatThrownBy(() -> transactionTemplate
                .executeWithoutResult(status -> gradingProposalRepository.save(duplicate)))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void jdbcWritesJoinApplicationJpaManagerAndRollbackTogether() {
        var created = seed();
        try (var interop = new AnnotationConfigApplicationContext(InteropConfig.class)) {
            var transactions = new TransactionTemplate(
                    interop.getBean(PlatformTransactionManager.class));
            var repository = interop.getBean(IEvaluationRepository.class);
            var visibility = interop.getBean(GradeDistributionVisibilityRepository.class);
            assertThatThrownBy(() -> transactions.executeWithoutResult(status -> {
                var value = repository.lockById(created.id).orElseThrow();
                value.feedback = "must rollback with caller";
                repository.save(value);
                visibility.updateVisibility(value.classId, value.assignmentId, true, 0);
                throw new IllegalStateException("caller/audit failure");
            })).hasMessageContaining("caller/audit failure");
            assertThat(visibility.findVisibility(created.classId, created.assignmentId).version())
                    .isZero();
        }
        assertThat(read(created.id).feedback).isNull();
        assertThat(read(created.id).version).isEqualTo(created.version);
    }

    @Configuration
    @EnableTransactionManagement
    @Import({ EvaluationRepositoryAdapter.class, GradingProposalRepositoryAdapter.class,
            GradeDistributionVisibilityRepository.class })
    static class InteropConfig {
        @Bean
        DataSource dataSource() {
            return context.getBean(DataSource.class);
        }

        @Bean
        NamedParameterJdbcTemplate namedTemplate(DataSource source) {
            return new NamedParameterJdbcTemplate(source);
        }

        @Bean
        JdbcTemplate jdbcTemplate(DataSource source) {
            return new JdbcTemplate(source);
        }

        @Bean
        org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean entityManagerFactory(
                DataSource source) {
            var factory = new org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(source);
            factory.setPackagesToScan("edu.aiplatform.subjects.repository");
            factory.setJpaVendorAdapter(
                    new org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter());
            factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "validate"));
            return factory;
        }

        @Bean
        PlatformTransactionManager transactionManager(
                jakarta.persistence.EntityManagerFactory factory) {
            return new org.springframework.orm.jpa.JpaTransactionManager(factory);
        }
    }

    @Test
    void explicitNoOpSavesAdvanceVersionsButReadsDoNot() {
        var created = seed();
        var saved = transactionTemplate.execute(status -> evaluationRepository.save(read(created.id)));
        assertThat(saved.version).isEqualTo(created.version + 1);
        assertThat(read(created.id).version).isEqualTo(saved.version);
        var pending = proposal(created, 1);
        transactionTemplate.executeWithoutResult(status -> gradingProposalRepository.save(pending));
        transactionTemplate.executeWithoutResult(status ->
                gradingProposalRepository.save(gradingProposalRepository.findById(pending.id).orElseThrow()));
        assertThat(new JdbcTemplate(context.getBean(DataSource.class)).queryForObject(
                "select version from grading_proposals where id=?", Long.class, pending.id)).isEqualTo(1);
        gradingProposalRepository.findById(pending.id);
        assertThat(new JdbcTemplate(context.getBean(DataSource.class)).queryForObject(
                "select version from grading_proposals where id=?", Long.class, pending.id)).isEqualTo(1);
    }

    private GradingProposal proposal(Evaluation evaluation, long version) {
        var proposal = new GradingProposal();
        proposal.id = UUID.randomUUID();
        proposal.evaluationId = evaluation.id;
        proposal.versionNo = version;
        proposal.requestRef = "fixture-" + proposal.id;
        proposal.requesterId = evaluation.accountId;
        proposal.createdAt = NOW;
        return proposal;
    }

    @Test
    void committedVersionsAllowSaveFinalizePublishAndStillRejectAStaleWriter() {
        var created = seed();
        var rubricId = UUID.randomUUID();
        var itemId = UUID.randomUUID();
        var structured = new GradingFeedback(1,
                new GradingFeedback.Summary(List.of("Đúng cấu trúc"), List.of("Cần bổ sung"),
                        List.of("Ôn tập chương 2")),
                List.of(new GradingFeedback.Item(rubricId, itemId, "Minh chứng", "Giải thích",
                        "Gợi ý", "q1", false, null)));
        var saved = transactionTemplate.execute(status -> {
            var e = evaluationRepository.lockById(created.id).orElseThrow();
            e.checkVersion(created.version);
            e.saveGrade("{}", BigDecimal.TEN, BigDecimal.ZERO, null, "QA", null, UUID.randomUUID(),
                    NOW);
            e.structuredFeedback = structured;
            e.reviewedItemScores = Map.of(rubricId, Map.of(itemId, BigDecimal.TEN));
            e.acknowledgedReviewItems = Map.of(rubricId, Set.of(itemId));
            return evaluationRepository.save(e);
        });
        assertThat(saved.version).isEqualTo(created.version + 1);
        var reloaded = read(saved.id);
        assertThat(reloaded.version).isEqualTo(saved.version);
        assertThat(reloaded.structuredFeedback).isEqualTo(structured);
        assertThat(reloaded.reviewedItemScores.get(rubricId).get(itemId))
                .isEqualByComparingTo("10");
        assertThat(reloaded.acknowledgedReviewItems.get(rubricId)).containsExactly(itemId);
        var finalized = transactionTemplate.execute(status -> {
            var e = evaluationRepository.lockById(saved.id).orElseThrow();
            e.checkVersion(saved.version);
            e.finalizeGrade(UUID.randomUUID(), NOW);
            return evaluationRepository.save(e);
        });
        assertThat(finalized.version).isEqualTo(saved.version + 1);
        assertThat(read(finalized.id).version).isEqualTo(finalized.version);
        var published = transactionTemplate.execute(status -> {
            var e = evaluationRepository.lockById(finalized.id).orElseThrow();
            e.checkVersion(finalized.version);
            e.publishGrade(UUID.randomUUID(), NOW);
            return evaluationRepository.save(e);
        });
        assertThat(published.version).isEqualTo(finalized.version + 1);
        assertThat(read(published.id).version).isEqualTo(published.version);
        assertThatThrownBy(
                () -> transactionTemplate.execute(status -> evaluationRepository.save(saved)))
                .isInstanceOf(ApiException.class).extracting("code")
                .isEqualTo("GRADE_VERSION_CONFLICT");
        assertThat(read(published.id).state.name()).isEqualTo("PUBLISHED");
    }
}
