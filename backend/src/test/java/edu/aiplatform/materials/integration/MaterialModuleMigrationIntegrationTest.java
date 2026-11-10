package edu.aiplatform.materials.integration;

import static org.assertj.core.api.Assertions.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

/** Actual U13 migrations; dependency tables are minimal FK fixtures, not composed unit evidence. */
@Testcontainers(disabledWithoutDocker = true)
class MaterialModuleMigrationIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> DATABASE = new PostgreSQLContainer<>("postgres:16-alpine")
            .withPassword(UUID.randomUUID().toString());

    @Test
    void backfillPreservesMaterialsAndQuizLinksAreUniqueAndCascadeOnModuleDeletion() {
        var dataSource = new DriverManagerDataSource(DATABASE.getJdbcUrl(), DATABASE.getUsername(),
                DATABASE.getPassword());
        var jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("create role app nologin");
        for (String table : new String[] {"accounts", "subjects", "course_classes", "quizzes"}) {
            jdbcTemplate.execute("create table " + table + " (id uuid primary key)");
        }
        UUID actor = UUID.randomUUID(), subject = UUID.randomUUID(), material = UUID.randomUUID();
        UUID quiz = UUID.randomUUID(), secondModule = UUID.randomUUID();
        jdbcTemplate.update("insert into accounts(id) values (?)", actor);
        jdbcTemplate.update("insert into subjects(id) values (?)", subject);
        jdbcTemplate.update("insert into quizzes(id) values (?)", quiz);
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration/materials")
                .baselineOnMigrate(true).baselineVersion("0").target("20260925.1900")
                .load().migrate();
        Timestamp createdAt = Timestamp.from(Instant.parse("2026-10-01T00:00:00Z"));
        jdbcTemplate.update("""
                insert into materials(id,scope,subject_id,kind,title,description,order_no,status,
                    current_version_no,created_by,created_at,updated_at,version)
                values (?,'SUBJECT',?,'FILE','Original title','Original description',3,'ARCHIVED',
                    1,?,?,?,7)
                """, material, subject, actor, createdAt, createdAt);
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration/materials")
                .baselineOnMigrate(true).baselineVersion("0").load().migrate();
        UUID defaultModule = jdbcTemplate.queryForObject("select module_id from materials where id=?",
                UUID.class, material);
        assertThat(jdbcTemplate.queryForObject("select title from material_modules where id=?",
                String.class, defaultModule)).isEqualTo("Module 1");
        assertThat(jdbcTemplate.queryForMap("select title,description,order_no,status,version from materials where id=?",
                material)).containsEntry("title", "Original title")
                .containsEntry("description", "Original description").containsEntry("order_no", 3)
                .containsEntry("status", "ARCHIVED").containsEntry("version", 7L);
        jdbcTemplate.update("""
                insert into material_modules(id,subject_id,title,order_no,created_by,created_at,updated_at)
                values (?,?,'Second',1,?,?,?)
                """, secondModule, subject, actor, createdAt, createdAt);
        jdbcTemplate.update("insert into material_module_quizzes(module_id,quiz_id,order_no,created_at) values (?,?,0,?)",
                secondModule, quiz, createdAt);
        assertThatThrownBy(() -> jdbcTemplate.update(
                "insert into material_module_quizzes(module_id,quiz_id,order_no,created_at) values (?,?,0,?)",
                defaultModule, quiz, createdAt)).isInstanceOf(DataIntegrityViolationException.class);
        jdbcTemplate.update("delete from material_modules where id=?", secondModule);
        assertThat(jdbcTemplate.queryForObject("select count(*) from material_module_quizzes", Integer.class))
                .isZero();
        assertThat(jdbcTemplate.queryForObject("select count(*) from materials where id=?", Integer.class, material))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select count(*) from quizzes where id=?", Integer.class, quiz))
                .isEqualTo(1);
    }
}
