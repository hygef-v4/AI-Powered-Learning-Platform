package edu.aiplatform.quizzes.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import edu.aiplatform.ai.contract.McpToolDefinition;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Tham số mẫu khớp {@code parameterSchema} của tool MCP U10 theo JSON Schema 2020-12, cùng thư viện U06 dùng để kiểm
 * trước khi gọi tool (M4).
 */
class QuizMcpToolsSchemaTest {

    private final ObjectMapper json = new ObjectMapper();
    private final Map<String, JsonSchema> schemas = new QuizMcpTools(null, json).tools().stream()
            .collect(Collectors.toMap(McpToolDefinition::name, d -> JsonSchemaFactory
                    .getInstance(SpecVersion.VersionFlag.V202012).getSchema(d.parameterSchema())));

    private boolean valid(String tool, String args) throws Exception {
        JsonNode node = json.readTree(args);
        return schemas.get(tool).validate(node).isEmpty();
    }

    @Test
    void readQuizContextAcceptsOnlyQuizId() throws Exception {
        String id = UUID.randomUUID().toString();
        assertThat(valid("read_quiz_context", "{\"quizId\":\"" + id + "\"}")).isTrue();
        assertThat(valid("read_quiz_context", "{}")).isFalse();
        assertThat(valid("read_quiz_context", "{\"quizId\":\"" + id + "\",\"answers\":true}")).isFalse();
    }

    @Test
    void saveQuizDraftAcceptsTheDocumentedQuestionShape() throws Exception {
        String ok = """
                {"quizId":"%s","expectedVersion":3,"agentLabel":"Claude Desktop","questions":[
                  {"type":"MCQ_MULTI","stem":"Số nguyên tố","options":["2","3","4"],"correct":[1,2],"points":2},
                  {"type":"MCQ_SINGLE","stem":"1 + 1?","options":["2","3"],"correct":[1],"explanation":"Cộng",
                   "difficulty":"EASY"}]}""".formatted(UUID.randomUUID());
        assertThat(valid("save_quiz_draft", ok)).isTrue();
        assertThat(valid("save_quiz_draft", ok.replace("MCQ_MULTI", "ESSAY"))).isFalse();
        assertThat(valid("save_quiz_draft", ok.replace("\"points\":2", "\"points\":2,\"answer\":1"))).isFalse();
        assertThat(valid("save_quiz_draft", ok.replace("\"expectedVersion\":3,", ""))).isFalse();
        assertThat(valid("save_quiz_draft", """
                {"quizId":"%s","expectedVersion":0,"questions":[]}""".formatted(UUID.randomUUID()))).isFalse();
    }
}
