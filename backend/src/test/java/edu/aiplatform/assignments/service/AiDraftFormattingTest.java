package edu.aiplatform.assignments.service;

import static org.assertj.core.api.Assertions.assertThat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.aiplatform.assignments.contract.*;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AiDraftFormattingTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void rejectsUnsafeStylesAndMismatchedRunText() throws Exception {
        var format = AiDraftFormatting.normalize(json.readTree("""
                {"align":"absolute","color":"url(secret)","html":"<script>","bold":"true",
                 "runs":[{"text":"Different","bold":true}]}
                """), "Original");
        assertThat(format.isEmpty()).isTrue();
        assertThat(json.readTree(AiDraftProposal.OUTPUT_SCHEMA).at("/properties/sections/items/properties/guidanceFormat/properties/align").isObject()).isTrue();
        assertThat(json.readTree(AssignmentDraftRevision.OUTPUT_SCHEMA).at("/properties/operations/items/properties/format").isObject()).isTrue();
    }
}
