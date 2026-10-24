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
}
