package edu.aiplatform.regression;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.assignments.contract.*;
import edu.aiplatform.assignments.service.DocumentModelService;
import edu.aiplatform.grading.model.Evaluation;
import edu.aiplatform.grading.service.GradingPromptBuilder;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;

class GradingDiagramIntegrationTest {
    private final ObjectMapper json = new ObjectMapper();
    private final DocumentModelService documents = new DocumentModelService(json);
    private final GradingPromptBuilder prompts = new GradingPromptBuilder(documents, json);
    private static final String EMPTY = "<mxGraphModel><root><mxCell id=\"0\"/><mxCell id=\"1\" parent=\"0\"/></root></mxGraphModel>";

    private DocumentBlock diagram(UUID id, BlockOrigin origin, String xml) {
        return new DocumentBlock(id, origin, BlockType.DIAGRAM, json.createObjectNode().put("xml", xml));
    }

    private String captured(int index) throws Exception {
        try (var input = getClass().getResourceAsStream("/grading/drawio-compressed-" + index + ".drawio")) {
            return new String(Objects.requireNonNull(input).readAllBytes(), StandardCharsets.UTF_8).strip();
        }
    }

    @Test
    void diagramOnlyAnswersKeepAllThreeRubricsAndTheirContentInTheProviderPrompt() throws Exception {
        UUID assignmentId = UUID.randomUUID();
        var skeletonBlocks = new ArrayList<DocumentBlock>();
        var submittedBlocks = new ArrayList<DocumentBlock>();
        var rubrics = new ArrayList<RubricView>();
        for (int i = 0; i < 3; i++) {
            var heading = new DocumentBlock(UUID.randomUUID(), BlockOrigin.TEACHER, BlockType.HEADING,
                    json.createObjectNode().put("style", "H1").put("text", "Question " + i));
            skeletonBlocks.add(heading);
            submittedBlocks.add(heading);
            submittedBlocks.add(diagram(UUID.randomUUID(), BlockOrigin.STUDENT, captured(i)));
            BigDecimal max = BigDecimal.valueOf(i == 0 ? 2 : 4);
            rubrics.add(new RubricView(UUID.randomUUID(), assignmentId, heading.id().toString(),
                    List.of(new RubricCriterion(UUID.randomUUID(), "Design", "", CriterionKind.TEXT,
                            List.of(new RubricItem(UUID.randomUUID(), "Diagram evidence", max)))), max));
        }
        var skeleton = new DocumentModel(skeletonBlocks);
        var submitted = new DocumentModel(submittedBlocks);
        assertThat(prompts.findEmptyPartIds(submitted, skeleton)).isEmpty();
        assertThat(prompts.findEmptyPartIds(submitted, skeleton, documents.computeParts(skeleton))).isEmpty();
        assertThat(prompts.findEmptyRubrics(submitted, skeleton, rubrics)).isEmpty();
        var active = prompts.findActiveRubrics(submitted, skeleton, rubrics);
        assertThat(active).containsExactlyElementsOf(rubrics);
        var content = new AssignmentContent(assignmentId, 1, "Grade the diagrams", skeleton,
                documents.computeParts(skeleton), Map.of(), rubrics);
        var payload = json.readTree(prompts.buildPrompt(new Evaluation(), submitted, content, active, Set.of()).user());
        assertThat(payload.path("rubrics")).hasSize(3);
        assertThat(payload.path("submission").toString()).contains("Customer", "BookingController", "findOverlapping");
    }

    @Test
    void anEditedCompressedTeacherDiagramCountsButUnchangedOrEmptySlotsDoNot() throws Exception {
        UUID id = UUID.randomUUID();
        var original = new DocumentModel(List.of(diagram(id, BlockOrigin.TEACHER, EMPTY)));
        var edited = new DocumentModel(List.of(diagram(id, BlockOrigin.TEACHER, captured(1))));
        assertThat(prompts.findEmptyPartIds(edited, original)).isEmpty();
        assertThat(prompts.findEmptyPartIds(original, original)).containsExactly("DOCUMENT");
        var compressedEmpty = new DocumentModel(List.of(diagram(id, BlockOrigin.STUDENT,
                "<mxfile><diagram>UzV2zq1wL0osyPDNT0nNUTV2VTV2LsrPL4GwciucU3NyVI0MMlNUjV1UjYwMVI2MVI3ccMgagmUNChKLUvNKsGhQNXJDmK1q5IZmNQA=</diagram></mxfile>")));
        assertThat(prompts.findEmptyPartIds(compressedEmpty, original)).containsExactly("DOCUMENT");
        assertThat(prompts.findEmptyPartIds(edited, edited)).containsExactly("DOCUMENT");
        var invalid = new DocumentModel(List.of(diagram(id, BlockOrigin.STUDENT, "<mxfile><diagram>invalid</diagram></mxfile>")));
        assertThat(prompts.findEmptyPartIds(invalid, original)).containsExactly("DOCUMENT");
    }
}
