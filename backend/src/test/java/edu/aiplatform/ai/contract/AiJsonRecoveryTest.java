package edu.aiplatform.ai.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class AiJsonRecoveryTest {

    private static final String SCHEMA = """
            {"type":"object","required":["summary","parts"],"properties":{
             "summary":{"type":"string"},
             "parts":{"type":"array","items":{"type":"object","properties":{"partId":{"type":"string"}}}},
             "detail":{"anyOf":[{"type":"object","properties":{"note":{"type":"string"}}},{"type":"null"}]}}}
            """;
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void jsonEncodedAsTextAtTheRootIsReadAgain() throws Exception {
        String inner = "{\"summary\":\"Khá\",\"parts\":[]}";
        String output = json.writeValueAsString(inner);

        JsonNode recovered = AiJsonRecovery.recoverOutput(json, output, json.readTree(SCHEMA));

        assertThat(recovered).isEqualTo(json.readTree(inner));
        assertThat(AiJsonRecovery.recoverOutputText(json, output, SCHEMA)).isEqualTo(json.readTree(inner).toString());
    }

    @Test
    void codeFenceAndLeadingProseAreRemoved() throws Exception {
        JsonNode expected = json.readTree("{\"summary\":\"Tốt\",\"parts\":[]}");

        assertThat(AiJsonRecovery.recoverOutput(json, "```json\n{\"summary\":\"Tốt\",\"parts\":[]}\n```",
                json.readTree(SCHEMA))).isEqualTo(expected);
        assertThat(AiJsonRecovery.recoverOutput(json, "Kết quả: {\"summary\":\"Tốt\",\"parts\":[]} Hết.",
                json.readTree(SCHEMA))).isEqualTo(expected);
    }

    @Test
    void nestedFieldsSentAsTextFollowTheSchema() throws Exception {
        String output = "{\"summary\":\"{\\\"x\\\":1}\",\"parts\":\"[{\\\"partId\\\":\\\"P1\\\"}]\","
                + "\"detail\":\"{\\\"note\\\":\\\"n\\\"}\"}";

        JsonNode recovered = AiJsonRecovery.recoverOutput(json, output, json.readTree(SCHEMA));

        assertThat(recovered.path("summary").isTextual()).isTrue();
        assertThat(recovered.path("parts").get(0).path("partId").asText()).isEqualTo("P1");
        assertThat(recovered.path("detail").path("note").asText()).isEqualTo("n");
    }

    @Test
    void plainTextAndValidOutputAreLeftUnchanged() {
        assertThat(AiJsonRecovery.recoverOutput(json, "Bài làm tốt, không có JSON", null)).isNull();
        assertThat(AiJsonRecovery.recoverOutputText(json, "Bài làm tốt", SCHEMA)).isEqualTo("Bài làm tốt");
        String valid = "{\"summary\":\"A\",\"parts\":[],\"score\":2.50}";
        assertThat(AiJsonRecovery.recoverOutputText(json, valid, SCHEMA)).isSameAs(valid);
        assertThat(AiJsonRecovery.recoverOutputText(json, "```json\n{}\n```", null)).isEqualTo("```json\n{}\n```");
    }
}
