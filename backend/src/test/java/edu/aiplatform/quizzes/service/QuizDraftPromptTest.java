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
    void schemaIsJsonAndCapsQuestionCount() throws Exception {
        JsonNode schema = json.readTree(QuizDraftPrompt.schema(7));

        assertThat(schema.at("/properties/questions/maxItems").asInt()).isEqualTo(7);
        assertThat(schema.at("/properties/questions/items/properties/options/maxItems").asInt()).isEqualTo(6);
        assertThat(QuizDraftPrompt.maxOutputTokens(50)).isEqualTo(16_000);
    }

    @Test
    void questionsMapOneBasedAnswersToOptionIds() throws Exception {
        List<DraftQuestion> parsed = QuizDraftPrompt.parseQuestions(json.readTree("""
                [{"type":"mcq_multi","stem":" Số chẵn ","options":["2","3","4"],"correct":[1,3],
                  "difficulty":"hard","explanation":null}]"""));

        DraftQuestion q = parsed.get(0);
        assertThat(q.content().type()).isEqualTo(QuestionType.MCQ_MULTI);
        assertThat(q.content().stem()).isEqualTo("Số chẵn");
        assertThat(q.content().options()).extracting(o -> o.id()).containsExactly("1", "2", "3");
        assertThat(q.content().correctOptionIds()).containsExactly("1", "3");
        assertThat(q.content().difficulty()).isEqualTo(Difficulty.HARD);
        assertThat(q.violations()).isEmpty();
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
