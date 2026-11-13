package edu.aiplatform.ai.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.sun.net.httpserver.HttpServer;
import edu.aiplatform.ai.service.IProviderGateway.FailureKind;
import edu.aiplatform.ai.service.IProviderGateway.ProviderException;
import edu.aiplatform.ai.service.IProviderGateway.Request;
import edu.aiplatform.ai.service.IProviderGateway.Response;
import edu.aiplatform.ai.contract.AiImage;
import edu.aiplatform.ai.contract.AiProvider;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Adapter gọi HTTP server giả của JDK thay cho provider thật (NFR-U06-40). */
class HttpProviderGatewayTest {
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(AiProvider.class)
    void tokenTruncationIsAnExplicitChargedFailureEvenWhenThePartialJsonParses(AiProvider provider) {
        reply = switch (provider) {
            case GEMINI -> "{\"candidates\":[{\"finishReason\":\"MAX_TOKENS\",\"content\":{\"parts\":[{\"text\":\"{}\"}]}}],\"usageMetadata\":{\"candidatesTokenCount\":500}}";
            case OPENAI -> "{\"choices\":[{\"finish_reason\":\"length\",\"message\":{\"content\":\"{}\"}}],\"usage\":{\"completion_tokens\":500}}";
            case CLAUDE -> "{\"stop_reason\":\"max_tokens\",\"content\":[{\"type\":\"text\",\"text\":\"{}\"}],\"usage\":{\"output_tokens\":500}}";
        };
        assertThatThrownBy(() -> gateway.call(provider, request(SCHEMA)))
                .isInstanceOfSatisfying(ProviderException.class, e -> {
                    assertThat(e.kind()).isEqualTo(FailureKind.CHARGED);
                    assertThat(e.usage().outputTokens()).isEqualTo(500);
                    assertThat(e.getMessage()).contains("giới hạn token", "phản hồi").doesNotContain("hạn mức");
                });
    }

    @Test
    void quotaAndRateLimitMessagesUseExplicitProviderCodesWithoutExposingTheBody() {
        assertThat(gateway.safeMessage(429,
                "{\"error\":{\"code\":\"insufficient_quota\",\"message\":\"secret-key\"}}"))
                .contains("hạn mức", "số dư").doesNotContain("secret-key", "tần suất");
        assertThat(gateway.safeMessage(429,
                "{\"error\":{\"code\":\"rate_limit_exceeded\"}}"))
                .contains("tần suất").doesNotContain("hết hạn mức");
        assertThat(gateway.safeMessage(429, "{}"))
                .contains("tần suất hoặc hết hạn mức");
        assertThat(gateway.safeMessage(429, " ")).contains("HTTP 429");
    }

    @Test
    void reachingTheTokenCountWithoutAnExplicitTruncationReasonDoesNotImplyFailure() {
        reply = "{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":\"{}\"}}],\"usage\":{\"completion_tokens\":500}}";
        assertThat(gateway.call(AiProvider.OPENAI, request(SCHEMA)).output()).isEqualTo("{}");
    }


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
        assertThat(response.usage().outputTokens()).as("token suy nghĩ cũng tính vào token ra").isEqualTo(35);
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
    void emptyOutputNamesTheProviderStopReasonInsteadOfAGenericMessage() {
        // Production 2026-11-12: Gemini trả 200 không có chữ, người dùng chỉ thấy "không trả nội dung".
        reply = "{\"candidates\":[{\"finishReason\":\"RECITATION\"}],\"usageMetadata\":{\"promptTokenCount\":900}}";
        assertThatThrownBy(() -> gateway.call(AiProvider.GEMINI, request(SCHEMA)))
                .isInstanceOfSatisfying(ProviderException.class, e -> {
                    assertThat(e.kind()).isEqualTo(FailureKind.CHARGED);
                    assertThat(e.usage().inputTokens()).isEqualTo(900);
                    assertThat(e.getMessage()).contains("dừng", "RECITATION", "trùng văn bản có sẵn");
                });

        reply = "{\"promptFeedback\":{\"blockReason\":\"PROHIBITED_CONTENT\"},\"usageMetadata\":{\"promptTokenCount\":900}}";
        assertThatThrownBy(() -> gateway.call(AiProvider.GEMINI, request(SCHEMA)))
                .hasMessageContaining("chặn nội dung gửi lên").hasMessageContaining("PROHIBITED_CONTENT");

        reply = "{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":null,\"refusal\":\"secret\"}}]}";
        assertThatThrownBy(() -> gateway.call(AiProvider.OPENAI, request(SCHEMA)))
                .hasMessageContaining("model từ chối trả lời").hasMessageNotContaining("secret");

        reply = "{\"choices\":[{\"finish_reason\":\"content_filter\",\"message\":{\"content\":\"\"}}]}";
        assertThatThrownBy(() -> gateway.call(AiProvider.OPENAI, request(SCHEMA)))
                .hasMessageContaining("content_filter: bộ lọc nội dung chặn");

        reply = "{\"stop_reason\":\"refusal\",\"content\":[]}";
        assertThatThrownBy(() -> gateway.call(AiProvider.CLAUDE, request(SCHEMA)))
                .hasMessageContaining("refusal: model từ chối trả lời");

        reply = "{\"candidates\":[{\"finishReason\":\"STOP\",\"content\":{\"parts\":[]}}]}";
        assertThatThrownBy(() -> gateway.call(AiProvider.GEMINI, request(SCHEMA)))
                .hasMessage(HttpProviderGateway.EMPTY_OUTPUT);
        assertThat(HttpProviderGateway.emptyOutputMessage("NEW_REASON", false))
                .isEqualTo("Nhà cung cấp AI dừng mà không trả nội dung (NEW_REASON)");
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

    private static Request imageRequest() {
        return new Request("model-x", "secret-key", "Hệ thống", "Chép đề", SCHEMA, 500, Duration.ofSeconds(2),
                List.of(new AiImage("image/jpeg", new byte[]{1, 2, 3}), new AiImage("image/png", new byte[]{4})));
    }

    @Test
    void geminiSendsImagesAsInlineDataBeforeTheText() throws Exception {
        reply = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{}\"}]}}]}";

        gateway.call(AiProvider.GEMINI, imageRequest());

        JsonNode parts = json.readTree(lastBody.get()).at("/contents/0/parts");
        assertThat(parts).hasSize(3);
        assertThat(parts.at("/0/inline_data/mime_type").asText()).isEqualTo("image/jpeg");
        assertThat(parts.at("/0/inline_data/data").asText()).isEqualTo("AQID");
        assertThat(parts.at("/1/inline_data/mime_type").asText()).isEqualTo("image/png");
        assertThat(parts.at("/2/text").asText()).isEqualTo("Chép đề");
    }

    @Test
    void openAiSendsImagesAsDataUrlContentParts() throws Exception {
        reply = "{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":\"{}\"}}]}";

        gateway.call(AiProvider.OPENAI, imageRequest());

        JsonNode content = json.readTree(lastBody.get()).at("/messages/1/content");
        assertThat(content).hasSize(3);
        assertThat(content.at("/0/type").asText()).isEqualTo("image_url");
        assertThat(content.at("/0/image_url/url").asText()).isEqualTo("data:image/jpeg;base64,AQID");
        assertThat(content.at("/0/image_url/detail").asText()).isEqualTo("high");
        assertThat(content.at("/2/type").asText()).isEqualTo("text");
        assertThat(content.at("/2/text").asText()).isEqualTo("Chép đề");
    }

    @Test
    void claudeSendsImagesAsBase64Blocks() throws Exception {
        reply = "{\"stop_reason\":\"tool_use\",\"content\":[{\"type\":\"tool_use\",\"name\":\"return_result\",\"input\":{}}]}";

        gateway.call(AiProvider.CLAUDE, imageRequest());

        JsonNode content = json.readTree(lastBody.get()).at("/messages/0/content");
        assertThat(content).hasSize(3);
        assertThat(content.at("/0/type").asText()).isEqualTo("image");
        assertThat(content.at("/0/source/type").asText()).isEqualTo("base64");
        assertThat(content.at("/0/source/media_type").asText()).isEqualTo("image/jpeg");
        assertThat(content.at("/0/source/data").asText()).isEqualTo("AQID");
        assertThat(content.at("/2/text").asText()).isEqualTo("Chép đề");
    }

    @Test
    void requestsWithoutImagesKeepThePlainTextUserMessage() throws Exception {
        reply = "{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":\"{}\"}}]}";

        gateway.call(AiProvider.OPENAI, request(SCHEMA));

        assertThat(json.readTree(lastBody.get()).at("/messages/1/content").isTextual()).isTrue();
        assertThat(request(SCHEMA).toString()).contains("images=0").doesNotContain("secret-key");
        assertThat(imageRequest().toString()).contains("images=2");
    }

    @Test
    void modelThatRejectsImagesIsReportedWithoutExposingTheBody() {
        status = 400;
        reply = "{\"error\":{\"message\":\"This model does not support image input secret-detail\"}}";

        assertThatThrownBy(() -> gateway.call(AiProvider.OPENAI, imageRequest()))
                .isInstanceOfSatisfying(ProviderException.class, e -> {
                    assertThat(e.kind()).isEqualTo(FailureKind.REJECTED);
                    assertThat(e.getMessage()).contains("không đọc được ảnh").doesNotContain("secret-detail");
                });
    }
}
