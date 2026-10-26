package edu.aiplatform.grading.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.ai.application.JsonSchemaChecker;
import edu.aiplatform.assignments.contract.CriterionKind;
import edu.aiplatform.assignments.contract.IDocumentModel;
import edu.aiplatform.assignments.contract.RubricCriterion;
import edu.aiplatform.assignments.contract.RubricItem;
import edu.aiplatform.assignments.contract.RubricView;
import edu.aiplatform.grading.domain.Evaluation;
import edu.aiplatform.shared.error.ApiException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GradingPromptSchemaTest {
    @Test
    void generatedSchemaDeclaresRubricKeysSectionPropertiesAndStructuredFeedback() throws Exception {
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
        var schemaText = builder.outputSchema(evaluation);
        var schema = json.readTree(schemaText);
        assertThat(schema.at("/properties/checks/properties/" + rubricId + "/items/enum/0").asText()).isEqualTo(itemId);
        assertThat(schema.at("/properties/checks/additionalProperties").asBoolean(true)).isFalse();
        assertThat(schema.at("/properties/sections/items/properties/partId/enum/0").asText()).isEqualTo("q1");
        assertThat(schema.at("/properties/sections/items/properties/feedback/type").asText()).isEqualTo("string");
        assertThat(schema.at("/properties/itemScoresByRubric/properties/" + rubricId + "/properties/" + itemId + "/maximum").decimalValue())
                .isEqualByComparingTo("2.5");
        assertThat(schema.path("required")).hasSize(6);
        assertThat(schema.at("/properties/structuredFeedback/properties/items/maxItems").asInt()).isEqualTo(1);
        assertThat(schema.at("/properties/structuredFeedback/properties/items/items/properties/rubricId/enum/0").asText())
                .isEqualTo(rubricId);
        assertThat(schema.at("/properties/structuredFeedback/properties/items/items/properties/itemId/enum/0").asText())
                .isEqualTo(itemId);

        var checker = new JsonSchemaChecker(json);
        assertThat(checker.validSchema(schemaText)).isTrue();
        String validOutput = """
            {
              "checks": {"%s": []},
              "itemScoresByRubric": {"%s": {"%s": 1.25}},
              "score": "1.25",
              "feedback": "Nhận xét tổng quát",
              "sections": [{"partId": "q1", "feedback": "Nhận xét phần 1"}],
              "structuredFeedback": {
                "schemaVersion": 1,
                "summary": {
                  "strengths": ["Xác định đúng các lớp chính"],
                  "improvements": ["Thiếu bội số quan hệ"],
                  "nextSteps": ["Bổ sung bội số 1..* trên liên kết"]
                },
                "items": [{
                  "rubricId": "%s",
                  "itemId": "%s",
                  "evidence": "Lớp LopHoc chưa ghi bội số với HocSinh",
                  "reason": "Đạt 1.25/2.50 điểm do thiếu bội số trên sơ đồ",
                  "suggestion": "Ghi rõ 1..* giữa LopHoc và HocSinh",
                  "evidenceBlockId": "q1",
                  "requiresReview": false,
                  "reviewReason": null
                }]
              }
            }
            """.formatted(rubricId, rubricId, itemId, rubricId, itemId);
        assertThat(checker.check(schemaText, validOutput)).isEmpty();

        // Regression: wire schema must reject the exact missing-evidence combination
        // rejected by the grading callback, while permitting explicit human review.
        var missingEvidence = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(validOutput);
        var feedbackItem = (com.fasterxml.jackson.databind.node.ObjectNode)
                missingEvidence.path("structuredFeedback").path("items").get(0);
        feedbackItem.putNull("evidence");
        assertThat(checker.check(schemaText, missingEvidence)).isPresent();
        feedbackItem.put("evidence", "");
        assertThat(checker.check(schemaText, missingEvidence)).isPresent();
        feedbackItem.putNull("evidence");
        feedbackItem.put("requiresReview", true);
        feedbackItem.put("reviewReason", "Không đọc được nội dung sơ đồ; cần giáo viên kiểm tra.");
        assertThat(checker.check(schemaText, missingEvidence)).isEmpty();
        var validatedReview = new GradingProposalValidator(json).validate(missingEvidence, GradingJson.rubrics(json,
                evaluation.rubricSnapshot), java.util.Set.of("q1"));
        assertThat(validatedReview.structuredFeedback().items().get(0).requiresReview()).isTrue();
        feedbackItem.put("reviewReason", "");
        assertThat(checker.check(schemaText, missingEvidence)).isPresent();
    }

    @Test
    void realisticMaxSupportedRubricScalesOutputTokensAndRejectsOversizedRubric() throws Exception {
        var json = new ObjectMapper().findAndRegisterModules();
        var builder = new GradingPromptBuilder(mock(IDocumentModel.class), json);
        var assignmentId = UUID.randomUUID();

        var thirtyItems = new ArrayList<RubricItem>();
        for (int i = 0; i < GradingPromptBuilder.MAX_AI_RUBRIC_ITEMS; i++) {
            thirtyItems.add(new RubricItem(UUID.randomUUID(), "Item " + i, BigDecimal.ONE));
        }
        var maxSupportedEval = new Evaluation();
        maxSupportedEval.rubricSnapshot = json.writeValueAsString(List.of(new RubricView(
                UUID.randomUUID(), assignmentId, "q1",
                List.of(new RubricCriterion(UUID.randomUUID(), "C", "", CriterionKind.values()[0], thirtyItems)),
                new BigDecimal("30"))));
        int tokens = builder.maxOutputTokens(maxSupportedEval);
        assertThat(tokens).isBetween(4096, 8192).isEqualTo(7624);
        assertThat(new JsonSchemaChecker(json).validSchema(builder.outputSchema(maxSupportedEval))).isTrue();

        var thirtyOneItems = new ArrayList<>(thirtyItems);
        thirtyOneItems.add(new RubricItem(UUID.randomUUID(), "Item 30", BigDecimal.ONE));
        var oversizedEval = new Evaluation();
        oversizedEval.rubricSnapshot = json.writeValueAsString(List.of(new RubricView(
                UUID.randomUUID(), assignmentId, "q1",
                List.of(new RubricCriterion(UUID.randomUUID(), "C", "", CriterionKind.values()[0], thirtyOneItems)),
                new BigDecimal("31"))));
        assertThatThrownBy(() -> builder.maxOutputTokens(oversizedEval))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("RUBRIC_TOO_LARGE_FOR_AI"));
        assertThatThrownBy(() -> builder.outputSchema(oversizedEval))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("RUBRIC_TOO_LARGE_FOR_AI"));
    }
}
