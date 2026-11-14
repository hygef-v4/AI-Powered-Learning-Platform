package edu.aiplatform.grading.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import edu.aiplatform.grading.service.GradingCommands.MemberSave;

class EvaluationJsonContractTest {
    @Test
    void memberSaveSupportsOldClientsAndNamedLevelsButRejectsUnknownOrOrdinalLevels() throws Exception {
        var json = new ObjectMapper();
        assertThat(json.readValue("{\"version\":0,\"score\":8}", MemberSave.class)
                .contributionLevel()).isNull();
        for (var level : ContributionLevel.values()) {
            var command = json.readValue("{\"version\":0,\"score\":8,\"contributionLevel\":\""
                    + level.name() + "\"}", MemberSave.class);
            assertThat(command.contributionLevel()).isEqualTo(level);
            assertThat(command.score()).isEqualByComparingTo("8");
        }
        for (var invalid : List.of("\"GOOD\"", "0", "1", "{}", "true")) {
            assertThatThrownBy(() -> json.readValue(
                    "{\"version\":0,\"score\":8,\"contributionLevel\":" + invalid + "}",
                    MemberSave.class)).isInstanceOf(com.fasterxml.jackson.core.JsonProcessingException.class);
        }
    }
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
