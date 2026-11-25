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
import edu.aiplatform.ai.config.AiProperties;
import edu.aiplatform.ai.model.AiConnection;
import edu.aiplatform.ai.service.ConnectionTestService;
import edu.aiplatform.ai.service.KeyCipher;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Adapter gọi HTTP server giả của JDK thay cho provider thật (NFR-U06-40). */
class HttpProviderGatewayTest {

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(AiProvider.class)
    void explicitTokenLimitProvesConnectionEvenWithoutVisibleOutput(AiProvider provider) {
        reply = switch (provider) {
            case GEMINI -> "{\"candidates\":[{\"finishReason\":\"MAX_TOKENS\"}],\"usageMetadata\":{\"thoughtsTokenCount\":1024}}";
            case OPENAI -> "{\"choices\":[{\"finish_reason\":\"length\",\"message\":{\"content\":\"\"}}],\"usage\":{\"completion_tokens\":1024}}";
            case CLAUDE -> "{\"stop_reason\":\"max_tokens\",\"content\":[],\"usage\":{\"output_tokens\":1024}}";
        };

        var result = testConnection(provider);

        assertThat(result.ok()).isTrue();
        assertThat(result.message()).isEqualTo("Đạt");
        assertThat(result.usage().outputTokens()).isEqualTo(1024);
    }

    private ConnectionTestService.Result testConnection(AiProvider provider) {
        AiProperties properties = new AiProperties(Base64.getEncoder().encodeToString(new byte[32]),
                null, null, Duration.ofSeconds(2), Map.of());
        KeyCipher cipher = new KeyCipher(properties);
        UUID id = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        AiConnection connection = AiConnection.createConnection(id, AiConnection.Scope.PERSONAL, ownerId, provider,
                "selected-model", cipher.encryptConnectionKey(id, AiConnection.Scope.PERSONAL, ownerId,
                        provider, "secret-key"), "-key", Instant.now());
        return new ConnectionTestService(gateway, cipher, properties).test(connection);
    }

    @Test
    void quotaAndRateLimitMessagesUseExplicitProviderCodesWithoutExposingTheBody() {
        assertThat(gateway.buildSafeMessage(429,
                "{\"error\":{\"code\":\"insufficient_quota\",\"message\":\"secret-key\"}}"))
                .contains("hạn mức", "số dư").doesNotContain("secret-key", "tần suất");
        assertThat(gateway.buildSafeMessage(429,
                "{\"error\":{\"code\":\"rate_limit_exceeded\"}}"))
                .contains("tần suất").doesNotContain("hết hạn mức");
        assertThat(gateway.buildSafeMessage(429, "{}"))
                .contains("tần suất hoặc hết hạn mức");
        assertThat(gateway.buildSafeMessage(429, " ")).contains("HTTP 429");
    }


    private static final String SCHEMA = "{\"type\":\"object\",\"properties\":{\"score\":{\"type\":\"number\"}}}";
    private final JsonMapper json = JsonMapper.builder().build();
    private HttpServer server;
    private HttpProviderGateway gateway;
    private final AtomicReference<String> lastBody = new AtomicReference<>();
    private final AtomicReference<String> lastPath = new AtomicReference<>();
    private final AtomicReference<String> lastAuth = new AtomicReference<>();
    private volatile int status = 200;
    private volatile String reply = "{}";
    private volatile long delayMillis = 0;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            lastPath.set(exchange.getRequestURI().getPath());
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

    private static Request reasoningRequest(String model, String effort) {
        return new Request(model, "secret-key", "Hệ thống", "Câu hỏi", SCHEMA, 11052, Duration.ofSeconds(2), List.of(),
                effort, 4096);
    }

    private static Request imageRequest() {
        return new Request("model-x", "secret-key", "Hệ thống", "Chép đề", SCHEMA, 500, Duration.ofSeconds(2),
                List.of(new AiImage("image/jpeg", new byte[]{1, 2, 3}), new AiImage("image/png", new byte[]{4})));
    }
}
