package edu.aiplatform.assignments.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.ai.application.JsonSchemaChecker;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.aiplatform.assignments.contract.GradingMode;
import java.math.BigDecimal;
import edu.aiplatform.assignments.contract.BlockType;
import edu.aiplatform.assignments.domain.PartCalculator;
import org.junit.jupiter.api.Test;

class AssignmentDraftGenerationSchemaTest {
    private final JsonSchemaChecker checker = new JsonSchemaChecker(new ObjectMapper());

    @Test
    void rejectsInstructionsOnlyWithEmptySections() {
        assertThat(checker.check(AiDraftProposal.OUTPUT_SCHEMA,
                "{\"instructions\":\"Read carefully.\",\"sections\":[]}")).isPresent();
    }

    @Test
    void generationRequiresExactQuestionsAndGradedRubricsBeforeBoundedProse() throws Exception {
        ObjectMapper json = new ObjectMapper();
        String schema = AssignmentDraftGenerationSchema.build(
                new AssignmentCommands.DraftOptions("AUTO", "EN", 1, new BigDecimal("10")), GradingMode.GRADED);
        ObjectNode proposal = (ObjectNode) json.readTree("""
                {"schemaVersion":2,"title":"Event exam","businessContext":"Organizers manage events.",
                 "instructions":"Work individually.","sections":[{"level":1,"title":"Model the domain",
                 "guidance":"Draw a class diagram.","rubric":[{"title":"Coverage","items":[{"text":"Domain relationships","points":10}]}]}]}
                """);
        assertThat(checker.check(schema, proposal)).isEmpty();
        assertThat(json.readTree(schema).path("properties").fieldNames().next()).isEqualTo("sections");
        ObjectNode section = (ObjectNode) proposal.path("sections").get(0);
        var rubric = section.remove("rubric");
        assertThat(checker.check(schema, proposal)).isPresent();
        section.putArray("rubric");
        assertThat(checker.check(schema, proposal)).isPresent();
        section.set("rubric", rubric);
        proposal.put("businessContext", "x".repeat(4001));
        assertThat(checker.check(schema, proposal)).isPresent();
        proposal.put("businessContext", "Valid context.");
        ((com.fasterxml.jackson.databind.node.ArrayNode) proposal.path("sections")).add(section.deepCopy());
        assertThat(checker.check(schema, proposal)).isPresent();
    }

    @Test
    void appliedContextKeepsParagraphsWithoutAddingGradedParts() {
        var proposal = new ObjectMapper().createObjectNode();
        proposal.put("businessContext", "Actors and rules.\n\nException flows.");
        proposal.putArray("sections").addObject().put("level", 1).put("title", "Question")
                .put("guidance", "First deliverable.\n\nSecond deliverable.");
        var built = AiDraftProposal.build(proposal);
        assertThat(built.skeleton().blocks()).filteredOn(block -> block.type() == BlockType.PARAGRAPH).hasSize(4);
        assertThat(PartCalculator.compute(built.skeleton())).hasSize(1);
    }
}
