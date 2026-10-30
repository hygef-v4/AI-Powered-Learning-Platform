package edu.aiplatform.quizzes.integration;

import edu.aiplatform.quizzes.repository.JdbcQuizRepository;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;
import edu.aiplatform.quizzes.contract.QuizScope;
import edu.aiplatform.quizzes.contract.QuizStatus;
import edu.aiplatform.quizzes.model.Quiz;
import edu.aiplatform.quizzes.model.QuizOrigin;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Upgrade from the legacy timestamp convention using only publication audit as positive provenance. */
@Testcontainers(disabledWithoutDocker = true)
class QuizPublicationMigrationIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    private static final Instant NOW = Instant.parse("2026-10-30T00:00:00Z");

    @Test
    void ambiguousLegacyRetirementNeedsSuccessfulQuizPublicationProofToReopenOpen() {
        try (HikariDataSource pool = new HikariDataSource()) {
            pool.setJdbcUrl(POSTGRES.getJdbcUrl());
            pool.setUsername(POSTGRES.getUsername());
            pool.setPassword(POSTGRES.getPassword());
            JdbcClient jdbc = JdbcClient.create(pool);
            jdbc.sql("create role app").update();
            Flyway.configure().dataSource(pool).locations("classpath:db/migration")
                    .target("20261029.1100").load().migrate();
            UUID author = UUID.randomUUID(), subject = UUID.randomUUID();
            jdbc.sql("insert into accounts (id,school_email,role,status,display_name) "
                            + "values (:id,'manager@example.test','SUBJECT_MANAGER','PENDING','Manager')")
                    .param("id", author).update();
            jdbc.sql("insert into subjects (id,code,name,status,manager_id,created_at,updated_at) "
                            + "values (:id,'CS101','Computing','ACTIVE',:manager,:now,:now)")
                    .param("id", subject).param("manager", author).param("now", java.sql.Timestamp.from(NOW)).update();
            JdbcQuizRepository quizzes = new JdbcQuizRepository(jdbc, new ObjectMapper());
            Quiz noProof = legacy(subject, author), proven = legacy(subject, author),
                    denied = legacy(subject, author), wrongType = legacy(subject, author);
            List.of(noProof, proven, denied, wrongType).forEach(quizzes::insert);
            proof(jdbc, proven.id(), "QUIZ", "SUCCESS");
            proof(jdbc, denied.id(), "QUIZ", "DENIED");
            proof(jdbc, wrongType.id(), "ASSIGNMENT", "SUCCESS");

            Flyway.configure().dataSource(pool).locations("classpath:db/migration").load().migrate();

            for (Quiz source : List.of(noProof, denied, wrongType)) {
                Quiz migrated = quizzes.find(source.id()).orElseThrow();
                assertThat(migrated.status()).isEqualTo(QuizStatus.RETIRED);
                assertThat(migrated.publishedAt()).as("unproven publication must not grant OPEN on reopen").isNull();
                assertThat(migrated.reopened(NOW).status()).isEqualTo(QuizStatus.DRAFT);
            }
            Quiz migratedPublished = quizzes.find(proven.id()).orElseThrow();
            assertThat(migratedPublished.publishedAt()).isEqualTo(NOW);
            assertThat(migratedPublished.reopened(NOW).status()).isEqualTo(QuizStatus.OPEN);
        }
    }

    private static Quiz legacy(UUID subject, UUID author) {
        return new Quiz(UUID.randomUUID(), QuizScope.SUBJECT, null, subject, null, QuizOrigin.MANUAL, 1,
                "Legacy retired Quiz", null, Quiz.DEFAULT_SETTINGS, false, null, List.of(), QuizStatus.RETIRED,
                NOW, NOW, "Archived", author, NOW, NOW, 0);
    }

    private static void proof(JdbcClient jdbc, UUID quiz, String type, String result) {
        jdbc.sql("insert into audit_logs (id,action,object_type,object_id,result,occurred_at) "
                        + "values (:id,'QUIZ_PUBLISHED',:type,:quiz,:result,:now)")
                .param("id", UUID.randomUUID()).param("type", type).param("quiz", quiz.toString())
                .param("result", result).param("now", java.sql.Timestamp.from(NOW)).update();
    }
}
