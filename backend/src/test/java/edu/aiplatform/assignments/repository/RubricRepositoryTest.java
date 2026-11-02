package edu.aiplatform.assignments.repository;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.assignments.contract.CriterionKind;
import edu.aiplatform.assignments.contract.RubricCriterion;
import edu.aiplatform.assignments.contract.RubricItem;
import edu.aiplatform.assignments.model.AssignmentRubric;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class RubricRepositoryTest {
    @Container static final PostgreSQLContainer<?> DB = new PostgreSQLContainer<>("postgres:16-alpine");
    final ObjectMapper json = new ObjectMapper();
    final UUID assignmentId = UUID.randomUUID();
    final List<RubricCriterion> criteria = List.of(new RubricCriterion(UUID.randomUUID(), "Architecture", "",
            CriterionKind.TEXT, List.of(new RubricItem(UUID.randomUUID(), "Clear responsibilities", new BigDecimal("2.50")))));
    JdbcTemplate jdbc;
    RubricRepository repository;

    @BeforeEach void setup() {
        var source = new DriverManagerDataSource(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword());
        jdbc = new JdbcTemplate(source);
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS assignment_rubrics (
                  id UUID PRIMARY KEY, assignment_id UUID NOT NULL, part_id TEXT NOT NULL,
                  criteria JSONB NOT NULL, total_points NUMERIC NOT NULL, locked_at TIMESTAMPTZ,
                  created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL)
                """);
        repository = new RubricRepository(JdbcClient.create(source), json);
    }

    @Test void currentArrayRoundTripsWithoutChangingCriteriaOrPoints() {
        var rubric = AssignmentRubric.empty(assignmentId, "part", Instant.now()).withCriteria(criteria, Instant.now());
        repository.insert(rubric);
        assertThat(repository.find(rubric.id()).orElseThrow().criteria()).isEqualTo(criteria);
        assertThat(repository.listByAssignment(assignmentId)).singleElement()
                .satisfies(value -> assertThat(value.totalPoints()).isEqualByComparingTo("2.50"));
    }

    @Test void historicalEnvelopeLoadsWithoutRewritingMetadataOrUnlockingRubric() throws Exception {
        String envelope = "{\"criteria\":" + json.writeValueAsString(criteria)
                + ",\"aiGenerated\":true,\"sampleAnswer\":\"Preserve this metadata\"}";
        var id = insert(envelope);
        var rubric = repository.listByAssignment(assignmentId).get(0);
        assertThat(rubric.criteria()).isEqualTo(criteria);
        assertThat(rubric.totalPoints()).isEqualByComparingTo("2.50");
        assertThat(rubric.locked()).isTrue();
        assertThat(rubric.version()).isEqualTo(7);
        assertThat(repository.find(id).orElseThrow().criteria()).isEqualTo(criteria);
        assertThat(jdbc.queryForObject("SELECT criteria = cast(? as jsonb) FROM assignment_rubrics WHERE id=?",
                Boolean.class, envelope, id)).isTrue();
    }

    @Test void malformedEnvelopeIsNotSilentlyTreatedAsAnEmptyRubric() {
        insert("{\"criteria\":{\"unexpected\":true}}");
        assertThatThrownBy(() -> repository.listByAssignment(assignmentId))
                .isInstanceOf(RuntimeException.class).hasStackTraceContaining("Không đọc được JSON của rubric");
    }

    UUID insert(String stored) {
        var id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO assignment_rubrics VALUES (?,?,'part',cast(? as jsonb),2.50,now(),now(),now(),7)
                """, id, assignmentId, stored);
        return id;
    }
}
