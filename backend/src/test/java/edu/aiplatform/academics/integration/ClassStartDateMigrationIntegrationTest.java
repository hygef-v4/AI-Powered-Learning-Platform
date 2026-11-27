package edu.aiplatform.academics.integration;

import edu.aiplatform.shared.testsupport.SharedPostgres;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Testcontainers;

/** D-U04-05 migration on disposable PostgreSQL: backfill, generated term and unique constraint. */
@Testcontainers(disabledWithoutDocker = true)
class ClassStartDateMigrationIntegrationTest {
    static final SharedPostgres.Database DATABASE = SharedPostgres.createDatabase();

    private final UUID subjectId = UUID.randomUUID();
    private DriverManagerDataSource dataSource;
    private JdbcTemplate jdbcTemplate;
    private String schema;

    @BeforeEach
    void createSchemaFixtures() {
        schema = "class_start_" + UUID.randomUUID().toString().replace("-", "");
        var owner = new JdbcTemplate(new DriverManagerDataSource(DATABASE.getJdbcUrl(),
                DATABASE.getUsername(), DATABASE.getPassword()));
        owner.execute("create schema " + schema);
        String separator = DATABASE.getJdbcUrl().contains("?") ? "&" : "?";
        dataSource = new DriverManagerDataSource(DATABASE.getJdbcUrl() + separator + "currentSchema="
                + schema, DATABASE.getUsername(), DATABASE.getPassword());
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("create table accounts (id uuid primary key)");
        jdbcTemplate.execute("create table subjects (id uuid primary key)");
        jdbcTemplate.update("insert into subjects (id) values (?)", subjectId);
    }

    private Flyway configureMigrations(String target) {
        var configuration = Flyway.configure().dataSource(dataSource).schemas(schema)
                .defaultSchema(schema).baselineOnMigrate(true).baselineVersion("0")
                .locations("classpath:db/migration/classes");
        if (target != null)
            configuration.target(target);
        return configuration.load();
    }

    private void insertLegacyClass(UUID id, String code, Instant createdAt) {
        jdbcTemplate.update("""
                insert into course_classes (id, subject_id, code, name, status, created_at,
                    updated_at, version)
                values (?, ?, ?, ?, 'DRAFT', ?, ?, 3)
                """, id, subjectId, code, code, Timestamp.from(createdAt),
                Timestamp.from(createdAt));
    }

    private void insertClass(String code, LocalDate startsOn) {
        Timestamp now = Timestamp.from(Instant.parse("2026-11-14T00:00:00Z"));
        jdbcTemplate.update("""
                insert into course_classes (id, subject_id, code, name, status, starts_on,
                    created_at, updated_at)
                values (?, ?, ?, ?, 'DRAFT', ?, ?, ?)
                """, UUID.randomUUID(), subjectId, code, code, Date.valueOf(startsOn), now, now);
    }

    @Test
    void backfillsLegacyClassesFromVietnamCreationDateWithoutTouchingVersion() {
        configureMigrations("20261107.2200").migrate();
        UUID boundary = UUID.randomUUID();
        UUID autumn = UUID.randomUUID();
        Instant boundaryCreated = Instant.parse("2026-03-31T17:30:00Z");
        insertLegacyClass(boundary, "C01", boundaryCreated);
        insertLegacyClass(autumn, "C02", Instant.parse("2026-09-15T03:00:00Z"));

        configureMigrations(null).migrate();

        var row = jdbcTemplate.queryForMap("""
                select starts_on, term, version, updated_at from course_classes where id = ?
                """, boundary);
        assertThat(((Date) row.get("starts_on")).toLocalDate()).isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(row.get("term")).isEqualTo("SP26-BL5");
        assertThat(row.get("version")).isEqualTo(3L);
        assertThat(((Timestamp) row.get("updated_at")).toInstant()).isEqualTo(boundaryCreated);
        assertThat(jdbcTemplate.queryForObject("select term from course_classes where id = ?",
                String.class, autumn)).isEqualTo("FA26");
    }

    @ParameterizedTest
    @CsvSource({
        "2027-01-10,SP27", "2027-04-03,SP27-BL5", "2027-05-15,SU27", "2027-08-20,SU27-BL5",
        "2026-09-07,FA26", "2026-12-01,FA26-BL5", "2000-01-01,SP00", "2099-12-31,FA99-BL5"
    })
    void generatedTermMatchesDomainRule(LocalDate startsOn, String expected) {
        configureMigrations(null).migrate();
        insertClass("C", startsOn);
        assertThat(jdbcTemplate.queryForObject("select term from course_classes", String.class))
                .isEqualTo(expected);
    }

    @Test
    void sameCodeIsAllowedAcrossTermsButNotWithinOneTerm() {
        configureMigrations(null).migrate();
        insertClass("SE1801", LocalDate.of(2027, 1, 10));
        insertClass("SE1801", LocalDate.of(2027, 4, 3));
        insertClass("SE1801", LocalDate.of(2027, 5, 15));
        assertThat(jdbcTemplate.queryForObject("select count(*) from course_classes",
                Integer.class)).isEqualTo(3);
        assertThatThrownBy(() -> insertClass("SE1801", LocalDate.of(2027, 2, 1)))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void rejectsMissingOrOutOfRangeStartDateAndManualTerm() {
        configureMigrations(null).migrate();
        Timestamp now = Timestamp.from(Instant.parse("2026-11-14T00:00:00Z"));
        assertThatThrownBy(() -> jdbcTemplate.update("""
                insert into course_classes (id, subject_id, code, name, created_at, updated_at)
                values (?, ?, 'C', 'C', ?, ?)
                """, UUID.randomUUID(), subjectId, now, now))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertClass("C", LocalDate.of(2100, 1, 1)))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbcTemplate.update("""
                insert into course_classes (id, subject_id, code, name, starts_on, term,
                    created_at, updated_at)
                values (?, ?, 'C', 'C', DATE '2027-01-10', 'SU27', ?, ?)
                """, UUID.randomUUID(), subjectId, now, now))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
    }
}
