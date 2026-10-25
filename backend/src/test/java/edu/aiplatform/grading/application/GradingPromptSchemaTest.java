package edu.aiplatform.grading.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.assignments.contract.IDocumentModel;
import edu.aiplatform.grading.domain.Evaluation;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class GradingPromptSchemaTest {
    @Test
    void generatedSchemaDeclaresRubricKeysAndSectionProperties() throws Exception {
        var json = new ObjectMapper().findAndRegisterModules();
        var rubricId = UUID.randomUUID().toString();
        var itemId = UUID.randomUUID().toString();
        var evaluation = new Evaluation();
        evaluation.rubricSnapshot = """
            [{"rubricId":"%s","assignmentId":"%s","partId":"q1","maxPoints":2.5,
              "criteria":[{"criterionId":"%s","title":"Design","kind":"TEXT","items":[
                {"itemId":"%s","text":"Correct","points":2.5}]}]}]
            """.formatted(rubricId, UUID.randomUUID(), UUID.randomUUID(), itemId);
        var builder = new GradingPromptBuilder(mock(IDocumentModel.class), json);
        var schema = json.readTree(builder.outputSchema(evaluation));
        assertThat(schema.at("/properties/checks/properties/" + rubricId + "/items/enum/0").asText()).isEqualTo(itemId);
        assertThat(schema.at("/properties/checks/additionalProperties").asBoolean(true)).isFalse();
        assertThat(schema.at("/properties/sections/items/properties/partId/enum/0").asText()).isEqualTo("q1");
        assertThat(schema.at("/properties/sections/items/properties/feedback/type").asText()).isEqualTo("string");
        assertThat(schema.at("/properties/itemScoresByRubric/properties/" + rubricId + "/properties/" + itemId + "/maximum").decimalValue()).isEqualByComparingTo("2.5");
        assertThat(schema.path("required")).hasSize(5);
    }
}
