package edu.aiplatform.academics.integration;

import static org.assertj.core.api.Assertions.assertThat;

import edu.aiplatform.academics.model.CourseClass;
import edu.aiplatform.academics.repository.CourseClassRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Real U05 migrations; minimal U01/U04 FK fixtures, disposable PostgreSQL only. */
@Testcontainers(disabledWithoutDocker = true)
class ClassTermMigrationIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> DATABASE = new PostgreSQLContainer<>("postgres:16-alpine")
            .withPassword(UUID.randomUUID().toString());

    private final UUID accountId = UUID.randomUUID();
    private final UUID subjectId = UUID.randomUUID();
    private final UUID classId = UUID.randomUUID();
    private final Instant createdAt = Instant.parse("2026-11-07T10:00:00Z");
    private DriverManagerDataSource dataSource;
    private JdbcTemplate jdbcTemplate;
    private String schema;

    @BeforeAll
    static void createApplicationRole() {
        new JdbcTemplate(new DriverManagerDataSource(DATABASE.getJdbcUrl(), DATABASE.getUsername(),
                DATABASE.getPassword())).execute("create role app");
    }

    @BeforeEach
    void createSchemaFixtures() {
        schema = "term_removal_" + UUID.randomUUID().toString().replace("-", "");
        var owner = new JdbcTemplate(new DriverManagerDataSource(DATABASE.getJdbcUrl(),
                DATABASE.getUsername(), DATABASE.getPassword()));
        owner.execute("create schema " + schema);
        String separator = DATABASE.getJdbcUrl().contains("?") ? "&" : "?";
        dataSource = new DriverManagerDataSource(DATABASE.getJdbcUrl() + separator + "currentSchema="
                + schema, DATABASE.getUsername(), DATABASE.getPassword());
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("create table accounts (id uuid primary key)");
        jdbcTemplate.execute("create table subjects (id uuid primary key)");
        jdbcTemplate.update("insert into accounts (id) values (?)", accountId);
        jdbcTemplate.update("insert into subjects (id) values (?)", subjectId);
    }

    private Flyway configureMigrations(String target) {
        var configuration = Flyway.configure().dataSource(dataSource).schemas(schema)
                .defaultSchema(schema).baselineOnMigrate(true).baselineVersion("0")
                .locations("classpath:db/migration/classes");
        if (target != null) configuration.target(target);
        return configuration.load();
    }

    private int countSemesterColumns() {
        return jdbcTemplate.queryForObject("""
                select count(*) from information_schema.columns
                where table_schema = ? and table_name = 'course_classes' and column_name = 'term'
                """, Integer.class, schema);
    }

    @Test
    void upgradeDropsOnlySemesterAndPreservesClassAndEnrollment() {
        configureMigrations("20261027.1100").migrate();
        jdbcTemplate.update("""
                insert into course_classes
                    (id, subject_id, code, name, description, term, status, teacher_id,
                     created_at, updated_at, version)
                values (?, ?, 'C01', 'Original class', 'Original description', '2026-1',
                        'OPEN', ?, ?, ?, 7)
                """, classId, subjectId, accountId, Timestamp.from(createdAt), Timestamp.from(createdAt));
        jdbcTemplate.update("""
                insert into enrollments (class_id, account_id, status, source, enrolled_at)
                values (?, ?, 'ACTIVE', 'MANUAL', ?)
                """, classId, accountId, Timestamp.from(createdAt));
        assertThat(countSemesterColumns()).isEqualTo(1);

        configureMigrations(null).migrate();

        assertThat(countSemesterColumns()).isZero();
        var repository = new CourseClassRepository(new NamedParameterJdbcTemplate(dataSource));
        var restored = repository.find(classId).orElseThrow();
        assertThat(restored.subjectId()).isEqualTo(subjectId);
        assertThat(restored.code()).isEqualTo("C01");
        assertThat(restored.name()).isEqualTo("Original class");
        assertThat(restored.description()).isEqualTo("Original description");
        assertThat(restored.status().name()).isEqualTo("OPEN");
        assertThat(restored.teacherId()).isEqualTo(accountId);
        assertThat(restored.createdAt()).isEqualTo(createdAt);
        assertThat(restored.updatedAt()).isEqualTo(createdAt);
        assertThat(restored.showGradeDistribution()).isFalse();
        assertThat(restored.version()).isEqualTo(7);
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from enrollments where class_id = ? and account_id = ?
                and status = 'ACTIVE' and source = 'MANUAL' and enrolled_at = ?
                """, Integer.class, classId, accountId, Timestamp.from(createdAt))).isEqualTo(1);

        new TransactionTemplate(new DataSourceTransactionManager(dataSource))
                .executeWithoutResult(status -> repository.update(
                        restored.assignTeacher(null, createdAt.plusSeconds(1)), 7));
        assertThat(repository.find(classId).orElseThrow().version()).isEqualTo(8);
        assertThat(configureMigrations(null).migrate().migrationsExecuted).isZero();
    }

    @Test
    void freshInstallationHasNoSemesterColumn() {
        configureMigrations(null).migrate();
        assertThat(countSemesterColumns()).isZero();
        assertThat(jdbcTemplate.queryForObject("select count(*) from course_classes", Integer.class))
                .isZero();
        assertThat(jdbcTemplate.queryForObject("select count(*) from enrollments", Integer.class))
                .isZero();
        var repository = new CourseClassRepository(new NamedParameterJdbcTemplate(dataSource));
        var fresh = CourseClass.createClass(classId, subjectId, "FRESH", "Fresh class", null,
                null, createdAt);
        new TransactionTemplate(new DataSourceTransactionManager(dataSource))
                .executeWithoutResult(status -> repository.insert(fresh));
        assertThat(repository.find(classId)).contains(fresh);
    }
}
