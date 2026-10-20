package edu.aiplatform.grading.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class GradingCommandsTest {
    final ObjectMapper json = new ObjectMapper();

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
}
