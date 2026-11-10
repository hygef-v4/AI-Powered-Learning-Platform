package edu.aiplatform.grading.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EvaluationJsonContractTest {
    @Test
    void internalGradePredicatesDoNotAddFieldsToTheEvaluationResponse() {
        var evaluation = Evaluation.createPendingEvaluation(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), "hash", BigDecimal.TEN, "[]", Instant.EPOCH);
        var objectMapper = new ObjectMapper().findAndRegisterModules();
        for (var state : List.of(EvaluationStatus.PENDING, EvaluationStatus.DRAFT,
                EvaluationStatus.FINALIZED, EvaluationStatus.PUBLISHED)) {
            evaluation.state = state;
            var response = objectMapper.valueToTree(evaluation);
            assertThat(response.path("state").asText()).isEqualTo(state.name());
            assertThat(response.has("official")).isFalse();
            assertThat(response.has("editable")).isFalse();
        }
    }
}
