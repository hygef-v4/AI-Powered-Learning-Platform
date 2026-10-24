package edu.aiplatform.ai.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.sun.net.httpserver.HttpServer;
import edu.aiplatform.ai.application.IProviderGateway.FailureKind;
import edu.aiplatform.ai.application.IProviderGateway.ProviderException;
import edu.aiplatform.ai.application.IProviderGateway.Request;
import edu.aiplatform.ai.application.IProviderGateway.Response;
import edu.aiplatform.ai.contract.AiProvider;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Adapter gọi HTTP server giả của JDK thay cho provider thật (NFR-U06-40). */
class HttpProviderGatewayTest {

    private static final String SCHEMA = "{\"type\":\"object\",\"properties\":{\"score\":{\"type\":\"number\"}}}";
    private final JsonMapper json = JsonMapper.builder().build();
    private HttpServer server;
    private HttpProviderGateway gateway;
    private final AtomicReference<String> lastBody = new AtomicReference<>();
    private final AtomicReference<String> lastAuth = new AtomicReference<>();
    private volatile int status = 200;
    private volatile String reply = "{}";
    private volatile long delayMillis = 0;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            lastAuth.set(auth != null ? auth : exchange.getRequestHeaders().getFirst("x-api-key") != null
                    ? "x-api-key:" + exchange.getRequestHeaders().getFirst("x-api-key")
                    : "x-goog-api-key:" + exchange.getRequestHeaders().getFirst("x-goog-api-key"));
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] bytes = reply.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        String base = "http://127.0.0.1:" + server.getAddress().getPort();
        gateway = new HttpProviderGateway(json, Map.of(AiProvider.GEMINI, base, AiProvider.OPENAI, base,
                AiProvider.CLAUDE, base));
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private static Request request(String schema) {
        return new Request("model-x", "secret-key", "Hệ thống", "Câu hỏi", schema, 500, Duration.ofSeconds(2));
    }

    @Test
    void geminiSendsSchemaAndNormalizesUsage() throws Exception {
        reply = "{\"responseId\":\"r1\",\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{\\\"score\\\":8}\"}]}}],"
                + "\"usageMetadata\":{\"promptTokenCount\":120,\"candidatesTokenCount\":30,\"thoughtsTokenCount\":5}}";
        Response response = gateway.call(AiProvider.GEMINI, request(SCHEMA));

        assertThat(response.output()).isEqualTo("{\"score\":8}");
        assertThat(response.requestId()).isEqualTo("r1");
        assertThat(response.usage().inputTokens()).isEqualTo(120);
        assertThat(response.usage().outputTokens()).isEqualTo(30);
        assertThat(response.usage().reasoningTokens()).isEqualTo(5);
        assertThat(response.usage().cachedTokens()).isNull();
        JsonNode body = json.readTree(lastBody.get());
        assertThat(body.at("/generationConfig/responseMimeType").asText()).isEqualTo("application/json");
        assertThat(body.at("/generationConfig/maxOutputTokens").asInt()).isEqualTo(500);
        assertThat(lastAuth.get()).isEqualTo("x-goog-api-key:secret-key");
    }

    @Test
    void geminiOmitsArrayMaximaFromWireSchemaOnly() throws Exception {
        String schema = """
                {"type":"object","required":["sections"],"properties":{"sections":{
                "type":"array","maxItems":100,"items":{"type":"object","properties":{"rubric":{
                "type":"array","maxItems":20,"items":{"type":"integer","minimum":1,"maximum":6}}}}}}}
                """;
        reply = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{}\"}]}}]}";
        Request input = request(schema);
        gateway.call(AiProvider.GEMINI, input);
        JsonNode wire = json.readTree(lastBody.get()).at("/generationConfig/responseJsonSchema");
        assertThat(wire.at("/properties/sections").has("maxItems")).isFalse();
        assertThat(wire.at("/properties/sections/items/properties/rubric").has("maxItems")).isFalse();
        assertThat(wire.at("/properties/sections/items/properties/rubric/items/minimum").asInt()).isEqualTo(1);
        assertThat(wire.path("required")).isEqualTo(json.readTree(schema).path("required"));
        assertThat(input.outputSchema()).isEqualTo(schema);
        assertThat(json.readTree(input.outputSchema()).at("/properties/sections/maxItems").asInt()).isEqualTo(100);
    }

    @Test
    void openAiUsesJsonSchemaFormatAndBearerKey() throws Exception {
        reply = "{\"id\":\"c1\",\"choices\":[{\"message\":{\"content\":\"{\\\"score\\\":7}\"}}],"
                + "\"usage\":{\"prompt_tokens\":50,\"completion_tokens\":10,\"prompt_tokens_details\":{\"cached_tokens\":20}}}";
        Response response = gateway.call(AiProvider.OPENAI, request(SCHEMA));

        assertThat(response.output()).isEqualTo("{\"score\":7}");
        assertThat(response.usage().cachedTokens()).isEqualTo(20);
        JsonNode body = json.readTree(lastBody.get());
        assertThat(body.at("/response_format/type").asText()).isEqualTo("json_schema");
        assertThat(body.at("/messages/0/role").asText()).isEqualTo("system");
        assertThat(lastAuth.get()).isEqualTo("Bearer secret-key");
    }

    @Test
    void claudeForcesResultToolAndReadsToolInput() throws Exception {
        reply = "{\"id\":\"m1\",\"content\":[{\"type\":\"tool_use\",\"name\":\"return_result\",\"input\":{\"score\":9}}],"
                + "\"usage\":{\"input_tokens\":70,\"output_tokens\":12}}";
        Response response = gateway.call(AiProvider.CLAUDE, request(SCHEMA));

        assertThat(json.readTree(response.output()).path("score").asInt()).isEqualTo(9);
        JsonNode body = json.readTree(lastBody.get());
        assertThat(body.at("/tool_choice/name").asText()).isEqualTo("return_result");
        assertThat(body.at("/max_tokens").asInt()).isEqualTo(500);
        assertThat(lastAuth.get()).isEqualTo("x-api-key:secret-key");
    }

    @Test
    void connectionTestWithoutSchemaReturnsPlainText() {
        reply = "{\"content\":[{\"type\":\"text\",\"text\":\"OK\"}],\"usage\":{\"input_tokens\":8,\"output_tokens\":1}}";
        assertThat(gateway.call(AiProvider.CLAUDE, request(null)).output()).isEqualTo("OK");
        assertThat(lastBody.get()).doesNotContain("tools");
    }

    @Test
    void geminiInvalidKeyIsReportedAsInvalidKeyNotBadModel() {
        status = 400;
        reply = "{\"error\":{\"code\":400,\"message\":\"API key not valid. Please pass a valid API key.\",\"status\":\"INVALID_ARGUMENT\",\"details\":[{\"reason\":\"API_KEY_INVALID\"}]}}";
        assertThatThrownBy(() -> gateway.call(AiProvider.GEMINI, request(null)))
                .isInstanceOfSatisfying(ProviderException.class, e -> {
                    assertThat(e.kind()).isEqualTo(FailureKind.REJECTED);
                    assertThat(e.getMessage()).isEqualTo("Key API không hợp lệ (HTTP 400)");
                });
        status = 400;
        reply = "{\"error\":{\"message\":\"Invalid model\"}}";
        assertThatThrownBy(() -> gateway.call(AiProvider.GEMINI, request(null)))
                .isInstanceOfSatisfying(ProviderException.class, e -> assertThat(e.getMessage()).contains("model"));
        status = 200;
    }

    @Test
    void clientErrorsAreRejectedWithoutChargeAndHideProviderBody() {
        status = 401;
        reply = "{\"error\":\"invalid key secret-key\"}";
        assertThatThrownBy(() -> gateway.call(AiProvider.OPENAI, request(SCHEMA)))
                .isInstanceOfSatisfying(ProviderException.class, e -> {
                    assertThat(e.kind()).isEqualTo(FailureKind.REJECTED);
                    assertThat(e.getMessage()).doesNotContain("secret-key").contains("401");
                });
        status = 429;
        assertThatThrownBy(() -> gateway.call(AiProvider.GEMINI, request(SCHEMA)))
                .isInstanceOfSatisfying(ProviderException.class, e -> assertThat(e.kind()).isEqualTo(FailureKind.REJECTED));
    }

    @Test
    void serverErrorsAndTimeoutsAreUnknown() {
        status = 500;
        assertThatThrownBy(() -> gateway.call(AiProvider.CLAUDE, request(SCHEMA)))
                .isInstanceOfSatisfying(ProviderException.class, e -> assertThat(e.kind()).isEqualTo(FailureKind.UNKNOWN));
        status = 200;
        delayMillis = 3000;
        assertThatThrownBy(() -> gateway.call(AiProvider.OPENAI, request(SCHEMA)))
                .isInstanceOfSatisfying(ProviderException.class, e -> assertThat(e.kind()).isEqualTo(FailureKind.UNKNOWN));
    }

    @Test
    void emptyOutputIsChargedWithUsage() {
        reply = "{\"choices\":[{\"message\":{\"content\":\"\"}}],\"usage\":{\"prompt_tokens\":50,\"completion_tokens\":500}}";
        assertThatThrownBy(() -> gateway.call(AiProvider.OPENAI, request(SCHEMA)))
                .isInstanceOfSatisfying(ProviderException.class, e -> {
                    assertThat(e.kind()).isEqualTo(FailureKind.CHARGED);
                    assertThat(e.usage().outputTokens()).isEqualTo(500);
                });
    }

    @Test
    void overloadIsRejectedAndUnreachableProviderIsRejected() {
        assertThat(HttpProviderGateway.kindOf(529)).isEqualTo(FailureKind.REJECTED);
        assertThat(HttpProviderGateway.kindOf(503)).isEqualTo(FailureKind.REJECTED);
        assertThat(HttpProviderGateway.kindOf(502)).isEqualTo(FailureKind.UNKNOWN);
        server.stop(0);
        assertThatThrownBy(() -> gateway.call(AiProvider.GEMINI, request(SCHEMA)))
                .isInstanceOfSatisfying(ProviderException.class, e -> assertThat(e.kind()).isEqualTo(FailureKind.REJECTED));
    }
}
