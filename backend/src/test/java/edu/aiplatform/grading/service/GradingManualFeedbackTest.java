package edu.aiplatform.grading.service;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.assignments.contract.*;
import edu.aiplatform.grading.model.GradingFeedback;
import edu.aiplatform.shared.error.ApiException;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class GradingManualFeedbackTest {
    private final ObjectMapper json = new ObjectMapper();
    private final GradingFeedbackValidator validator = new GradingFeedbackValidator(json);
    private final UUID rubricId = UUID.randomUUID(), itemId = UUID.randomUUID();
    private final List<RubricView> rubrics = List.of(new RubricView(rubricId, UUID.randomUUID(), "part",
            List.of(new RubricCriterion(UUID.randomUUID(), "Design", "", CriterionKind.TEXT,
                    List.of(new RubricItem(itemId, "Item", BigDecimal.ONE)))), BigDecimal.ONE));

    private GradingFeedback feedback(String reason, String evidence, String blockId) {
        return new GradingFeedback(1, new GradingFeedback.Summary(List.of(), List.of(), List.of()),
                List.of(new GradingFeedback.Item(rubricId, itemId, evidence, reason, null,
                        blockId, false, null)));
    }

    @Test
    void manualNodeAllowsOptionalFieldsWhileAiStillRequiresExplanationAndEvidence() {
        var optional = feedback("", null, null);
        assertThat(validator.validateNode(json.valueToTree(optional), rubrics, Set.of("part"), false))
                .isEqualTo(optional);
        assertThatThrownBy(() -> validator.validateFeedback(optional, rubrics, Set.of("part"), true))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> validator.validateFeedback(
                feedback("Explanation", null, null), rubrics, Set.of("part"), true))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void optionalManualFieldsStillValidateAnchorsAndTextLimits() {
        assertThatThrownBy(() -> validator.validateFeedback(
                feedback("", null, "unknown"), rubrics, Set.of("part"), false))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> validator.validateFeedback(
                feedback("x".repeat(1001), null, null), rubrics, Set.of("part"), false))
                .isInstanceOf(ApiException.class);
    }
}
