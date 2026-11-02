package edu.aiplatform.grading.infrastructure;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.grading.domain.Evaluation;
import edu.aiplatform.grading.domain.GradingFeedback;
import edu.aiplatform.grading.domain.GradingProposal;
import edu.aiplatform.grading.domain.IEvaluationRepository;
import edu.aiplatform.grading.domain.IGradingProposalRepository;
import edu.aiplatform.shared.error.ApiException;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

/** Real Hibernate JSON dirty checking and committed versions, in a disposable database only. */
@Testcontainers(disabledWithoutDocker = true)
class EvaluationVersionIntegrationTest {
    @Container static final PostgreSQLContainer<?> DB = new PostgreSQLContainer<>("postgres:16-alpine");
    static AnnotationConfigApplicationContext context;
    static IEvaluationRepository repository;
    static IGradingProposalRepository proposals;
    static TransactionTemplate tx;
    static final Instant NOW = Instant.parse("2026-10-23T11:00:00Z");

    @Configuration @EnableTransactionManagement
    @EnableJpaRepositories(basePackageClasses = {IEvaluationJpaRepository.class, IGradingProposalJpaRepository.class})
    @Import({EvaluationRepositoryAdapter.class, GradingProposalRepositoryAdapter.class})
    static class Config {
        @Bean DataSource dataSource() {
            return new DriverManagerDataSource(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword());
        }
        @Bean LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource source) {
            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(source);
            factory.setPackagesToScan(EvaluationJpaEntity.class.getPackageName());
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop",
                    "hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy"));
            return factory;
        }
        @Bean PlatformTransactionManager transactionManager(EntityManagerFactory factory) {
            return new JpaTransactionManager(factory);
        }
    }

    @BeforeAll static void setup() {
        context = new AnnotationConfigApplicationContext(Config.class);
        repository = context.getBean(IEvaluationRepository.class);
        proposals = context.getBean(IGradingProposalRepository.class);
        tx = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        tx.setTimeout(10);
    }
    @AfterAll static void close() { if (context != null) context.close(); }

    Evaluation seed() {
        var e = Evaluation.pending(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "hash", BigDecimal.TEN, "[]", NOW);
        return tx.execute(status -> repository.save(e));
    }
    Evaluation read(UUID id) { return tx.execute(status -> repository.findById(id).orElseThrow()); }

    @Test void readingJsonDoesNotWriteOrInvalidateVersion() {
        var created = seed();
        assertThat(new JdbcTemplate(context.getBean(DataSource.class)).queryForObject(
                "select jsonb_typeof(grading_document) from evaluations where id=?",String.class,created.id)).isEqualTo("object");
        assertThat(read(created.id).version).isEqualTo(created.version);
        assertThat(read(created.id).version).isEqualTo(created.version);
    }

    @Test void pollingProposalsInsideTransactionDoesNotDirtyWriteOrIncrementProposalVersion() throws Exception {
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
        tx.execute(status -> proposals.save(p));

        assertThat(jdbc.queryForObject("select jsonb_typeof(proposal_document) from grading_proposals where id=?",
                String.class, p.id)).isEqualTo("object");
        Long initialVersion = jdbc.queryForObject("select version from grading_proposals where id=?", Long.class, p.id);
        assertThat(initialVersion).isZero();

        // Simulate repeated polling transactions (lock evaluation + read proposals).
        for (int i = 0; i < 5; i++) {
            var listed = tx.execute(status -> {
                repository.lockById(created.id).orElseThrow();
                return proposals.findForEvaluation(created.id);
            });
            assertThat(listed).singleElement().satisfies(found -> assertThat(found.status).isEqualTo("PENDING"));
        }
        Long versionAfterPolls = jdbc.queryForObject("select version from grading_proposals where id=?", Long.class, p.id);
        assertThat(versionAfterPolls).as("read polls must not dirty-write grading_proposals").isEqualTo(initialVersion);

        // Callback delivery updates proposal to READY in its transaction cleanly.
        UUID taskId = UUID.randomUUID();
        var payload = json.readTree("{\"checks\":{},\"score\":8.5,\"feedback\":\"Tốt\",\"sections\":[]}");
        var delivered = tx.execute(status -> {
            repository.lockById(created.id).orElseThrow();
            var match = proposals.findForEvaluation(created.id).get(0);
            match.taskId = taskId;
            match.status = "READY";
            match.proposal = payload;
            match.completedAt = NOW.plusSeconds(3);
            return proposals.save(match);
        });
        assertThat(delivered.status).isEqualTo("READY");
        assertThat(delivered.taskId).isEqualTo(taskId);
        Long versionAfterDelivery = jdbc.queryForObject("select version from grading_proposals where id=?", Long.class, p.id);
        assertThat(versionAfterDelivery).isEqualTo(initialVersion + 1);
    }

    @Test void committedVersionsAllowSaveFinalizePublishAndStillRejectAStaleWriter() {
        var created = seed();
        var rubricId = UUID.randomUUID();
        var itemId = UUID.randomUUID();
        var structured = new GradingFeedback(
                1,
                new GradingFeedback.Summary(List.of("Đúng cấu trúc"), List.of("Cần bổ sung"), List.of("Ôn tập chương 2")),
                List.of(new GradingFeedback.Item(rubricId, itemId, "Minh chứng", "Giải thích", "Gợi ý", "q1", false, null)));
        var saved = tx.execute(status -> {
            var e = repository.lockById(created.id).orElseThrow();
            e.checkVersion(created.version);
            e.save("{}", BigDecimal.TEN, BigDecimal.ZERO, null, "QA", null, UUID.randomUUID(), NOW);
            e.structuredFeedback = structured;
            e.reviewedItemScores = Map.of(rubricId, Map.of(itemId, BigDecimal.TEN));
            e.acknowledgedReviewItems = Map.of(rubricId, Set.of(itemId));
            return repository.save(e);
        });
        assertThat(saved.version).isEqualTo(created.version+1);
        var reloaded = read(saved.id);
        assertThat(reloaded.version).isEqualTo(saved.version);
        assertThat(reloaded.structuredFeedback).isEqualTo(structured);
        assertThat(reloaded.reviewedItemScores.get(rubricId).get(itemId)).isEqualByComparingTo("10");
        assertThat(reloaded.acknowledgedReviewItems.get(rubricId)).containsExactly(itemId);
        var finalized = tx.execute(status -> {
            var e = repository.lockById(saved.id).orElseThrow();
            e.checkVersion(saved.version);
            e.finalizeGrade(UUID.randomUUID(), NOW);
            return repository.save(e);
        });
        assertThat(finalized.version).isEqualTo(saved.version+1);
        assertThat(read(finalized.id).version).isEqualTo(finalized.version);
        var published = tx.execute(status -> {
            var e = repository.lockById(finalized.id).orElseThrow();
            e.checkVersion(finalized.version);
            e.publish(UUID.randomUUID(), NOW);
            return repository.save(e);
        });
        assertThat(published.version).isEqualTo(finalized.version+1);
        assertThat(read(published.id).version).isEqualTo(published.version);
        assertThatThrownBy(() -> tx.execute(status -> repository.save(saved)))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("GRADE_VERSION_CONFLICT");
        assertThat(read(published.id).state.name()).isEqualTo("PUBLISHED");
    }
}
