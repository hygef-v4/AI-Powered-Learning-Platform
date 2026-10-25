package edu.aiplatform.grading.infrastructure;

import static org.assertj.core.api.Assertions.*;

import edu.aiplatform.grading.domain.Evaluation;
import edu.aiplatform.grading.domain.IEvaluationRepository;
import edu.aiplatform.shared.error.ApiException;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
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
    static TransactionTemplate tx;
    static final Instant NOW = Instant.parse("2026-10-23T11:00:00Z");

    @Configuration @EnableTransactionManagement
    @EnableJpaRepositories(basePackageClasses = IEvaluationJpaRepository.class)
    @Import(EvaluationRepositoryAdapter.class)
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

    @Test void committedVersionsAllowSaveFinalizePublishAndStillRejectAStaleWriter() {
        var created = seed();
        var saved = tx.execute(status -> {
            var e = repository.lockById(created.id).orElseThrow();
            e.checkVersion(created.version);
            e.save("{}", BigDecimal.TEN, BigDecimal.ZERO, null, "QA", null, UUID.randomUUID(), NOW);
            return repository.save(e);
        });
        assertThat(saved.version).isEqualTo(created.version+1);
        assertThat(read(saved.id).version).isEqualTo(saved.version);
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
