package edu.aiplatform.grading.application;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.assignments.contract.*;
import java.util.*;
import java.math.BigDecimal;

class GradingProposalValidatorTest {
    final ObjectMapper json = new ObjectMapper();
    final UUID rid = UUID.randomUUID(), iid = UUID.randomUUID();

    List<RubricView> rubric() {
        return List.of(new RubricView(
                rid, UUID.randomUUID(), "p", List.of(new RubricCriterion(UUID.randomUUID(), "c", "",
                        CriterionKind.values()[0], List.of(new RubricItem(iid, "i", BigDecimal.ONE)))),
                BigDecimal.ONE));
    }

    @Test
    void acceptsKnownChecklistAndRejectsUnknownIdsAndWrongTotals() {
        var validator = new GradingProposalValidator(json);
        var checks = Map.of(rid.toString(), List.of(iid.toString()));
        assertThat(validator.validate(
                json.valueToTree(Map.of("checks", checks, "score", "1", "feedback", "ok", "sections", List.of())),
                rubric()).score()).isEqualByComparingTo("1");
        assertThatThrownBy(() -> validator.validate(json.valueToTree(Map.of("checks", checks, "score", "2")), rubric()))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> validator.validate(
                json.valueToTree(
                        Map.of("checks", Map.of(rid.toString(), List.of(UUID.randomUUID().toString())), "score", "1")),
                rubric())).isInstanceOf(RuntimeException.class);
    }

    @Test
    void rejectsDuplicateChecksAndNonObjectPayload() {
        var validator = new GradingProposalValidator(json);
        assertThatThrownBy(
                () -> validator.validate(
                        json.valueToTree(Map.of("checks",
                                Map.of(rid.toString(), List.of(iid.toString(), iid.toString())), "score", "1")),
                        rubric()))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> validator.validate(json.valueToTree("not a proposal"), rubric()))
                .isInstanceOf(RuntimeException.class);
    }
}
