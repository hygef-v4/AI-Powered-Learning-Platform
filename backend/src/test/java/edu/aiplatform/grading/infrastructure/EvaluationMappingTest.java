package edu.aiplatform.grading.infrastructure;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import edu.aiplatform.grading.domain.*;
import java.util.UUID;
import java.time.Instant;
import java.math.BigDecimal;

class EvaluationMappingTest {
    @Test
    void aggregateMutationsCannotAutoFlushStaleIndexColumns() {
        var aggregate = Evaluation.pending(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "hash", BigDecimal.TEN, "[]", Instant.now());
        var row = new EvaluationJpaEntity();
        row.apply(aggregate);
        row.version = 3L;
        aggregate.feedback = "mutated after save";
        assertThat(row.view().feedback).isNull();
        var loaded = row.view();
        loaded.score = BigDecimal.ONE;
        loaded.state = EvaluationStatus.DRAFT;
        assertThat(loaded.version).isEqualTo(3);
        assertThat(row.view().score).isNull();
        assertThat(row.status).isEqualTo("PENDING");
        row.apply(loaded);
        assertThat(row.score).isEqualByComparingTo("1");
        assertThat(row.status).isEqualTo("DRAFT");
    }
    @Test
    void fractionalScoresRoundTripInsideExistingJsonDocumentAndOldRowsDefaultEmpty() throws Exception {
        var aggregate = Evaluation.pending(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "hash", BigDecimal.TEN, "[]", Instant.now());
        var rubric = UUID.randomUUID(); var item = UUID.randomUUID();
        aggregate.rubricItemScores = java.util.Map.of(rubric, java.util.Map.of(item, new BigDecimal("0.50")));
        var row = new EvaluationJpaEntity(); row.apply(aggregate); row.version = 1L;
        assertThat(row.view().rubricItemScores.get(rubric).get(item)).isEqualByComparingTo("0.50");
        var json = new com.fasterxml.jackson.databind.ObjectMapper();
        var old = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(row.document);
        old.remove("rubricItemScores"); row.document = old.toString();
        assertThat(row.view().rubricItemScores).isEmpty();
    }

}
