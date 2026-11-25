package edu.aiplatform.assignments.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.ai.service.JsonSchemaChecker;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.aiplatform.assignments.contract.GradingMode;
import java.math.BigDecimal;
import edu.aiplatform.assignments.contract.BlockType;
import edu.aiplatform.assignments.model.PartCalculator;
import org.junit.jupiter.api.Test;

class AssignmentDraftGenerationSchemaTest {
    private final JsonSchemaChecker checker = new JsonSchemaChecker(new ObjectMapper());

    @Test
    void rejectsInstructionsOnlyWithEmptySections() {
        assertThat(checker.validateValue(AiDraftProposal.OUTPUT_SCHEMA,
                "{\"instructions\":\"Read carefully.\",\"sections\":[]}")).isPresent();
    }
}
