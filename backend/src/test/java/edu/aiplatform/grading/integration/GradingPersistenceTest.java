package edu.aiplatform.grading.integration;

import java.sql.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Isolated SQL gate; does not stand in for complete U07/U08 migration
 * acceptance.
 */
@Testcontainers(disabledWithoutDocker = true)
class GradingPersistenceTest {
    @Container
    static final PostgreSQLContainer<?> DB = new PostgreSQLContainer<>("postgres:16-alpine");

    static Connection connection() throws SQLException {
        return DriverManager.getConnection(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword());
    }

    static String resource(String name) throws Exception {
        try (var in = GradingPersistenceTest.class.getResourceAsStream(name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @BeforeAll
    static void schema() throws Exception {
        try (var c = connection(); var s = c.createStatement()) {
            s.execute(resource("/grading/prerequisites.sql"));
            s.execute(resource("/db/migration/grading/V20260925_1500__create_grading_tables.sql"));
        }
    }

    static String seed(Connection c) throws SQLException {
        String id = UUID.randomUUID().toString();
        try (var s = c.createStatement()) {
            for (String table : new String[] { "accounts", "course_classes", "assignments", "assignment_attempts" })
                s.execute("INSERT INTO " + table + " VALUES ('" + id + "')");
        }
        return id;
    }

    static void insert(Connection c, String id, String score) throws SQLException {
        try (var p = c.prepareStatement(
                "INSERT INTO evaluations(id,assignment_id,class_id,attempt_id,account_id,kind,status,submission_hash,max_score,score,grading_document) VALUES (?, ?, ?, ?, ?, 'ATTEMPT','DRAFT','hash',10,?,'{}')")) {
            p.setObject(1, UUID.randomUUID());
            for (int i = 2; i <= 5; i++)
                p.setObject(i, UUID.fromString(id));
            p.setBigDecimal(6, new java.math.BigDecimal(score));
            p.executeUpdate();
        }
    }

    @Test
    void uniqueAttemptAndScoreConstraintsAreEnforced() throws Exception {
        try (var c = connection()) {
            String id = seed(c);
            insert(c, id, "4");
            assertThatThrownBy(() -> insert(c, id, "3")).isInstanceOf(SQLException.class);
            String other = seed(c);
            assertThatThrownBy(() -> insert(c, other, "11")).isInstanceOf(SQLException.class);
        }
    }

    @Test
    void rollbackRemovesSubmissionAndEvaluationTogether() throws Exception {
        String id;
        try (var c = connection()) {
            c.setAutoCommit(false);
            id = seed(c);
            insert(c, id, "4");
            c.rollback();
        }
        try (var c = connection(); var p = c.prepareStatement("SELECT count(*) FROM evaluations WHERE attempt_id=?")) {
            p.setObject(1, UUID.fromString(id));
            try (var result = p.executeQuery()) {
                result.next();
                assertThat(result.getInt(1)).isZero();
            }
        }
    }

    @Test
    void appCannotDeleteGradesOrProposals() throws Exception {
        try (var c = connection();
                var s = c.createStatement();
                var result = s.executeQuery(
                        "SELECT has_table_privilege('app','evaluations','UPDATE'),has_table_privilege('app','evaluations','DELETE'),has_table_privilege('app','grading_proposals','DELETE')")) {
            result.next();
            assertThat(result.getBoolean(1)).isTrue();
            assertThat(result.getBoolean(2)).isFalse();
            assertThat(result.getBoolean(3)).isFalse();
        }
    }
}
