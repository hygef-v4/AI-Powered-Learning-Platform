package edu.aiplatform.quizzes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.quizzes.contract.QuestionType;
import edu.aiplatform.quizzes.model.Difficulty;
import edu.aiplatform.quizzes.model.DraftQuestion;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Schema và cách đọc câu đề xuất (H5). */
class QuizDraftPromptTest {

    private final ObjectMapper json = new ObjectMapper();

    @Test
    void parsesRelevanceWithoutBreakingLegacyResultsAndRejectsWrongTypes() {
        String legacy = "{\"questions\":[{\"type\":\"MCQ_SINGLE\",\"stem\":\"x\",\"options\":[\"a\",\"b\"],\"correct\":[1]}]}";
        assertThat(QuizDraftPrompt.parseOutput(json, legacy).requestMatchesSources()).isTrue();
        assertThat(QuizDraftPrompt.parseOutput(json, legacy.replace("{\"questions\":",
                "{\"requestMatchesSources\":false,\"questions\":")).requestMatchesSources()).isFalse();
        assertThatThrownBy(() -> QuizDraftPrompt.parseOutput(json, legacy.replace("{\"questions\":",
                "{\"requestMatchesSources\":\"false\",\"questions\":"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> QuizDraftPrompt.parseOutput(json, legacy.replace("{\"questions\":",
                "{\"questionsMatchSources\":\"true\",\"questions\":"))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void schemaEnforcesTheRequestedTypeAndDifficulty() throws Exception {
        JsonNode item = json.readTree(QuizDraftPrompt.buildSchema(new QuizAiDraftService.DraftRequest("x", 3,
                QuestionType.MCQ_SINGLE, Difficulty.HARD, List.of(), null, null, "r"))).at("/properties/questions/items");

        assertThat(item.at("/properties/type/enum").toString()).isEqualTo("[\"MCQ_SINGLE\"]");
        assertThat(item.at("/properties/difficulty/enum").toString()).isEqualTo("[\"HARD\"]");
    }

    @Test
    void outOfRangeAnswerBecomesViolationButBrokenShapeIsRejected() throws Exception {
        assertThat(QuizDraftPrompt.parseQuestions(json.readTree("""
                [{"type":"MCQ_SINGLE","stem":"x","options":["a","b"],"correct":[3]}]""")).get(0).violations())
                .extracting(v -> v.code()).containsExactly("ANSWER_UNKNOWN");
        assertThatThrownBy(() -> QuizDraftPrompt.parseQuestions(json.readTree("""
                [{"type":"MCQ_SINGLE","stem":"x","options":["a","b"],"correct":[1],"answerKey":"a"}]""")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> QuizDraftPrompt.parseOutput(json, "[]")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> QuizDraftPrompt.parseQuestions(json.readTree("[]")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
