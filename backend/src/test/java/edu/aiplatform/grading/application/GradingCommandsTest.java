package edu.aiplatform.grading.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class GradingCommandsTest {
    final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    @Test
    void missingAndNullVersionCannotMasqueradeAsVersionZero() {
        for (String input : new String[] { "{}", "{\"version\":null}" })
            assertThatThrownBy(() -> json.readValue(input, GradingCommands.Save.class)).isInstanceOf(Exception.class);
    }

    @Test
    void clientTotalCannotBeSmuggledIntoSave() {
        assertThatThrownBy(() -> json.readValue("{\"version\":0,\"checkedItemsByRubric\":{},\"score\":10}",
                GradingCommands.Save.class)).isInstanceOf(Exception.class);
        assertThatCode(() -> json.readValue("{\"version\":0,\"checkedItemsByRubric\":{}}", GradingCommands.Save.class))
                .doesNotThrowAnyException();
    }

    @Test
    void unknownFieldsInsideStructuredFeedbackAreRejected() {
        assertThatThrownBy(() -> json.readValue("""
            {
              "version": 0,
              "checkedItemsByRubric": {},
              "structuredFeedback": {
                "schemaVersion": 1,
                "smuggledSubtotal": 9.5,
                "summary": {"strengths": [], "improvements": [], "nextSteps": []},
                "items": []
              }
            }
            """, GradingCommands.Save.class)).isInstanceOf(Exception.class);

        assertThatThrownBy(() -> json.readValue("""
            {
              "version": 0,
              "checkedItemsByRubric": {},
              "structuredFeedback": {
                "schemaVersion": 1,
                "summary": {"strengths": [], "improvements": [], "nextSteps": []},
                "items": [{
                  "rubricId": "00000000-0000-0000-0000-000000000001",
                  "itemId": "00000000-0000-0000-0000-000000000002",
                  "reason": "ok",
                  "requiresReview": false,
                  "confidencePercent": 90
                }]
              }
            }
            """, GradingCommands.Save.class)).isInstanceOf(Exception.class);
    }
}
