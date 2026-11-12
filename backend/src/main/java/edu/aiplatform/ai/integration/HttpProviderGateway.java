package edu.aiplatform.ai.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.aiplatform.ai.service.IProviderGateway;
import edu.aiplatform.ai.contract.AiImage;
import edu.aiplatform.ai.contract.AiProvider;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Gọi REST trực tiếp Gemini, OpenAI, Anthropic tới địa chỉ cố định (NFR-U06-32); ép kết quả JSON theo schema bằng
 * structured output của từng hãng; chuẩn hóa usage; phân loại lỗi (BR-U06-14). Không log key, prompt hay kết quả.
 */
@Component
public class HttpProviderGateway implements IProviderGateway {

    static final Map<AiProvider, String> BASE_URLS = Map.of(
            AiProvider.GEMINI, "https://generativelanguage.googleapis.com",
            AiProvider.OPENAI, "https://api.openai.com",
            AiProvider.CLAUDE, "https://api.anthropic.com");
    static final String RESULT_TOOL = "return_result";
    static final String IMAGES_NOT_SUPPORTED = "Model đang dùng không đọc được ảnh; hãy chọn model khác";

    private final ObjectMapper json;
    private final Map<AiProvider, String> baseUrls;
    private final HttpClient http;

    @Autowired
    public HttpProviderGateway(ObjectMapper json) {
        this(json, BASE_URLS);
    }

    /** Chỉ test dùng để trỏ tới HTTP server giả; người dùng không đổi được địa chỉ provider. */
    HttpProviderGateway(ObjectMapper json, Map<AiProvider, String> baseUrls) {
        this.json = json;
        this.baseUrls = baseUrls;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    @Override
    public Response call(AiProvider provider, Request request) {
        return switch (provider) {
            case GEMINI -> gemini(request);
            case OPENAI -> openAi(request);
            case CLAUDE -> claude(request);
        };
    }

    private Response gemini(Request r) {
        ObjectNode body = json.createObjectNode();
        if (r.system() != null) {
            body.putObject("systemInstruction").putArray("parts").addObject().put("text", r.system());
        }
        ArrayNode parts = body.putArray("contents").addObject().put("role", "user").putArray("parts");
        for (AiImage image : r.images()) {
            parts.addObject().putObject("inline_data").put("mime_type", image.mediaType()).put("data", base64(image));
        }
        parts.addObject().put("text", r.user());
        ObjectNode config = body.putObject("generationConfig");
        config.put("maxOutputTokens", r.maxOutputTokens());
        if (r.outputSchema() != null) {
            config.put("responseMimeType", "application/json");
            JsonNode schema = readSchema(r.outputSchema());
            omitGeminiArrayMaxima(schema);
            config.set("responseJsonSchema", schema);
        }
        String url = baseUrls.get(AiProvider.GEMINI) + "/v1beta/models/"
                + URLEncoder.encode(r.model(), StandardCharsets.UTF_8) + ":generateContent";
        JsonNode reply = send(HttpRequest.newBuilder(URI.create(url)).header("x-goog-api-key", r.apiKey()), body, r);
        JsonNode meta = reply.path("usageMetadata");
        // Gemini đếm token suy nghĩ riêng ngoài candidatesTokenCount nhưng vẫn tính tiền: gộp vào token ra như OpenAI
        // (completion_tokens đã gồm reasoning) để quota quyết toán đủ.
        Long thoughts = number(meta, "thoughtsTokenCount");
        Long candidates = number(meta, "candidatesTokenCount");
        Long output = candidates == null && thoughts == null ? null
                : (candidates == null ? 0 : candidates) + (thoughts == null ? 0 : thoughts);
        Usage usage = new Usage(number(meta, "promptTokenCount"), output, number(meta, "cachedContentTokenCount"),
                thoughts);
        requireCompleteOutput(reply.path("candidates").path(0).path("finishReason").asText(""), usage);
        StringBuilder text = new StringBuilder();
        for (JsonNode part : reply.path("candidates").path(0).path("content").path("parts")) {
            text.append(part.path("text").asText(""));
        }
        return response(text.toString(), reply.path("responseId").asText(null), usage);
    }

    private Response openAi(Request r) {
        ObjectNode body = json.createObjectNode();
        body.put("model", r.model());
        ArrayNode messages = body.putArray("messages");
        if (r.system() != null) {
            messages.addObject().put("role", "system").put("content", r.system());
        }
        if (r.images().isEmpty()) {
            messages.addObject().put("role", "user").put("content", r.user());
        } else {
            ArrayNode content = messages.addObject().put("role", "user").putArray("content");
            for (AiImage image : r.images()) {
                content.addObject().put("type", "image_url").putObject("image_url")
                        .put("url", "data:" + image.mediaType() + ";base64," + base64(image)).put("detail", "high");
            }
            content.addObject().put("type", "text").put("text", r.user());
        }
        body.put("max_completion_tokens", r.maxOutputTokens());
        if (r.outputSchema() != null) {
            ObjectNode format = body.putObject("response_format");
            format.put("type", "json_schema");
            ObjectNode schema = format.putObject("json_schema");
            schema.put("name", "result");
            schema.set("schema", readSchema(r.outputSchema()));
        }
        JsonNode reply = send(HttpRequest.newBuilder(URI.create(baseUrls.get(AiProvider.OPENAI) + "/v1/chat/completions"))
                .header("Authorization", "Bearer " + r.apiKey()), body, r);
        JsonNode u = reply.path("usage");
        Usage usage = new Usage(number(u, "prompt_tokens"), number(u, "completion_tokens"),
                number(u.path("prompt_tokens_details"), "cached_tokens"),
                number(u.path("completion_tokens_details"), "reasoning_tokens"));
        requireCompleteOutput(reply.path("choices").path(0).path("finish_reason").asText(""), usage);
        return response(reply.path("choices").path(0).path("message").path("content").asText(""),
                reply.path("id").asText(null), usage);
    }

    private Response claude(Request r) {
        ObjectNode body = json.createObjectNode();
        body.put("model", r.model());
        body.put("max_tokens", r.maxOutputTokens());
        if (r.system() != null) {
            body.put("system", r.system());
        }
        if (r.images().isEmpty()) {
            body.putArray("messages").addObject().put("role", "user").put("content", r.user());
        } else {
            ArrayNode content = body.putArray("messages").addObject().put("role", "user").putArray("content");
            for (AiImage image : r.images()) {
                content.addObject().put("type", "image").putObject("source").put("type", "base64")
                        .put("media_type", image.mediaType()).put("data", base64(image));
            }
            content.addObject().put("type", "text").put("text", r.user());
        }
        if (r.outputSchema() != null) {
            ObjectNode tool = body.putArray("tools").addObject();
            tool.put("name", RESULT_TOOL);
            tool.put("description", "Trả kết quả theo đúng schema");
            tool.set("input_schema", readSchema(r.outputSchema()));
            body.putObject("tool_choice").put("type", "tool").put("name", RESULT_TOOL);
        }
        JsonNode reply = send(HttpRequest.newBuilder(URI.create(baseUrls.get(AiProvider.CLAUDE) + "/v1/messages"))
                .header("x-api-key", r.apiKey()).header("anthropic-version", "2023-06-01"), body, r);
        JsonNode u = reply.path("usage");
        Usage usage = new Usage(number(u, "input_tokens"), number(u, "output_tokens"),
                number(u, "cache_read_input_tokens"), null);
        requireCompleteOutput(reply.path("stop_reason").asText(""), usage);
        String output = "";
        for (JsonNode block : reply.path("content")) {
            if ("tool_use".equals(block.path("type").asText()) && RESULT_TOOL.equals(block.path("name").asText())) {
                output = block.path("input").toString();
                break;
            }
            if ("text".equals(block.path("type").asText())) {
                output += block.path("text").asText("");
            }
        }
        return response(output, reply.path("id").asText(null), usage);
    }

    private JsonNode send(HttpRequest.Builder builder, ObjectNode body, Request r) {
        HttpRequest request = builder.timeout(r.timeout())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (HttpConnectTimeoutException | ConnectException e) {
            throw new ProviderException(FailureKind.REJECTED, 0, "Không kết nối được nhà cung cấp AI", null);
        } catch (HttpTimeoutException e) {
            throw new ProviderException(FailureKind.UNKNOWN, 0, "Nhà cung cấp AI không trả lời kịp", null);
        } catch (IOException e) {
            throw new ProviderException(FailureKind.UNKNOWN, 0, "Mất kết nối với nhà cung cấp AI", null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProviderException(FailureKind.UNKNOWN, 0, "Lời gọi AI bị dừng", null);
        }
        int status = response.statusCode();
        if (status >= 200 && status < 300) {
            try {
                return json.readTree(response.body());
            } catch (JsonProcessingException e) {
                throw new ProviderException(FailureKind.UNKNOWN, status, "Phản hồi của nhà cung cấp AI không đọc được",
                        null);
            }
        }
        if (status == 400 && !r.images().isEmpty() && mentionsImage(response.body())) {
            throw new ProviderException(FailureKind.REJECTED, status, IMAGES_NOT_SUPPORTED, null);
        }
        throw new ProviderException(kindOf(status), status, safeMessage(status, response.body()), null);
    }

    private static String base64(AiImage image) {
        return Base64.getEncoder().encodeToString(image.bytes());
    }

    /** Body chỉ dùng để phân loại lỗi model không nhận ảnh, không đưa vào thông báo (BR-U06-59, 60). */
    private static boolean mentionsImage(String body) {
        return body != null && body.toLowerCase(Locale.ROOT).contains("image");
    }

    /** 4xx và quá tải là từ chối, không tính token; lỗi 5xx khác không rõ đã xử lý chưa (BR-U06-14). */
    static FailureKind kindOf(int status) {
        if (status < 500 || status == 503 || status == 529) {
            return FailureKind.REJECTED;
        }
        return FailureKind.UNKNOWN;
    }

    /**
     * Gemini trả HTTP 400 (không phải 401) khi key sai; nhận ra qua mã lỗi trong body để báo đúng "key không hợp lệ".
     * Body chỉ dùng để phân loại, không đưa vào thông báo.
     */
    String safeMessage(int status, String body) {
        if (status == 400 && body != null && (body.contains("API_KEY_INVALID") || body.contains("API key not valid"))) {
            return "Key API không hợp lệ (HTTP 400)";
        }
        if (status == 429 && body != null && !body.isBlank()) {
            try {
                JsonNode error = json.readTree(body).path("error");
                String code = error.path("code").asText("");
                String type = error.path("type").asText("");
                if ("insufficient_quota".equals(code) || "insufficient_quota".equals(type)) {
                    return "Tài khoản API đã hết hạn mức hoặc số dư. Kiểm tra hạn mức và thanh toán của nhà cung cấp.";
                }
                if ("rate_limit_exceeded".equals(code) || "rate_limit_error".equals(type)) {
                    return "Nhà cung cấp AI đang giới hạn tần suất gọi. Vui lòng chờ rồi thử lại.";
                }
            } catch (JsonProcessingException ignored) {
                // HTTP429 alone cannot distinguish rate limits from exhausted quota.
            }
        }
        return safeMessage(status);
    }

    private static void requireCompleteOutput(String reason, Usage usage) {
        if ("MAX_TOKENS".equals(reason) || "length".equals(reason) || "max_tokens".equals(reason)) {
            throw new ProviderException(FailureKind.CHARGED, 200,
                    "AI đã đạt giới hạn token của lần gọi nên phản hồi chưa hoàn chỉnh. Hãy thử lại với bài nhỏ hơn hoặc cấu hình giới hạn đầu ra cao hơn.",
                    usage);
        }
    }

    static String safeMessage(int status) {
        return switch (status) {
            case 400 -> "Nhà cung cấp AI từ chối yêu cầu (HTTP 400): model hoặc tham số không hợp lệ";
            case 401, 403 -> "Key API không hợp lệ hoặc không có quyền (HTTP " + status + ")";
            case 404 -> "Model không tồn tại hoặc key không dùng được model này (HTTP 404)";
            case 429 -> "Nhà cung cấp AI báo vượt giới hạn tần suất hoặc hết hạn mức (HTTP 429)";
            case 503, 529 -> "Nhà cung cấp AI đang quá tải (HTTP " + status + ")";
            default -> "Nhà cung cấp AI lỗi (HTTP " + status + ")";
        };
    }

    private Response response(String output, String requestId, Usage usage) {
        if (output == null || output.isBlank()) {
            throw new ProviderException(FailureKind.CHARGED, 200, "Nhà cung cấp AI không trả nội dung", usage);
        }
        return new Response(output, requestId, usage);
    }

    /** Large nested array maxima exceed Gemini's decoding complexity limit.
     * Only the wire copy is relaxed; AiTaskJobHandler validates the original schema after generation.
     */
    private static void omitGeminiArrayMaxima(JsonNode schema) {
        if (schema instanceof ObjectNode object && "array".equals(object.path("type").asText())) {
            object.remove("maxItems");
        }
        schema.forEach(HttpProviderGateway::omitGeminiArrayMaxima);
    }

    private JsonNode readSchema(String schema) {
        try {
            return json.readTree(schema);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("outputSchema không phải JSON", e);
        }
    }

    private static Long number(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isNumber() ? value.asLong() : null;
    }
}
