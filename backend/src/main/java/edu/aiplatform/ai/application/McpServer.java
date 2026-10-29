package edu.aiplatform.ai.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.aiplatform.ai.application.McpCredentialService.McpPrincipal;
import edu.aiplatform.ai.contract.McpToolDefinition;
import edu.aiplatform.ai.contract.McpToolKind;
import edu.aiplatform.ai.contract.McpToolResult;
import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.shared.error.ApiException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Lõi JSON-RPC 2.0 của MCP server (F6; BR-U06-32–35): {@code initialize}, {@code ping}, {@code tools/list},
 * {@code tools/call}. Không gọi AI thay agent. Tham số và kết quả tool coi là không tin cậy; lỗi trả cho agent không
 * lộ dữ liệu.
 */
@Service
public class McpServer {

    public static final String LATEST_PROTOCOL = "2025-06-18";
    static final Set<String> PROTOCOLS = Set.of("2025-06-18", "2025-03-26");
    static final int MAX_ARGS_BYTES = 64 * 1024;
    static final int MAX_RESULT_BYTES = 256 * 1024;
    static final int MAX_TOOL_NAME = 100;
    static final String TRUNCATED = "\n…[đã cắt vì vượt 256 KB]";

    static final int PARSE_ERROR = -32700;
    static final int INVALID_REQUEST = -32600;
    static final int METHOD_NOT_FOUND = -32601;
    static final int INVALID_PARAMS = -32602;
    static final int RATE_LIMITED = -32029;

    private static final Logger log = LoggerFactory.getLogger(McpServer.class);

    private final McpToolRegistry registry;
    private final IMcpRateLimiter rateLimiter;
    private final JsonSchemaChecker schemas;
    private final AiUsageService usage;
    private final IAudit audit;
    private final ObjectMapper json;
    private final Clock clock;

    public McpServer(McpToolRegistry registry, IMcpRateLimiter rateLimiter, JsonSchemaChecker schemas,
                     AiUsageService usage, IAudit audit, ObjectMapper json, Clock clock) {
        this.registry = registry;
        this.rateLimiter = rateLimiter;
        this.schemas = schemas;
        this.usage = usage;
        this.audit = audit;
        this.json = json;
        this.clock = clock;
    }

    public static boolean supports(String protocolVersion) {
        return PROTOCOLS.contains(protocolVersion);
    }

    /** Thân lời gọi không phải JSON. */
    public ObjectNode parseError() {
        return error(null, PARSE_ERROR, "Thân yêu cầu không phải JSON");
    }

    /**
     * Xử lý một thông điệp; rỗng khi là notification hoặc response từ client (HTTP {@code 202}, không thân). Không
     * nhận batch (MCP 2025-06-18 bỏ batch).
     */
    public Optional<ObjectNode> handle(McpPrincipal principal, JsonNode message) {
        if (message == null || !message.isObject()) {
            return Optional.of(error(null, INVALID_REQUEST, "Cần một thông điệp JSON-RPC"));
        }
        JsonNode id = message.get("id");
        JsonNode method = message.get("method");
        if (!"2.0".equals(message.path("jsonrpc").asText(null))) {
            return Optional.of(error(id, INVALID_REQUEST, "Cần jsonrpc 2.0"));
        }
        if (method == null) {
            // response hoặc lỗi từ client cho yêu cầu của server; server này không gửi yêu cầu nên bỏ qua
            return message.has("result") || message.has("error") ? Optional.empty()
                    : Optional.of(error(id, INVALID_REQUEST, "Thiếu method"));
        }
        if (!method.isTextual()) {
            return Optional.of(error(id, INVALID_REQUEST, "method phải là chuỗi"));
        }
        if (id == null) {
            return Optional.empty();
        }
        if (!(id.isTextual() || id.isIntegralNumber())) {
            return Optional.of(error(null, INVALID_REQUEST, "id phải là chuỗi hoặc số"));
        }
        JsonNode params = message.path("params");
        return Optional.of(switch (method.asText()) {
            case "initialize" -> result(id, initialize(params));
            case "ping" -> result(id, json.createObjectNode());
            case "tools/list" -> result(id, toolsList(principal));
            case "tools/call" -> toolsCall(principal, id, params);
            default -> error(id, METHOD_NOT_FOUND, "Không hỗ trợ method này");
        });
    }

    private ObjectNode initialize(JsonNode params) {
        String requested = params.path("protocolVersion").asText("");
        ObjectNode out = json.createObjectNode();
        out.put("protocolVersion", PROTOCOLS.contains(requested) ? requested : LATEST_PROTOCOL);
        out.putObject("capabilities").putObject("tools").put("listChanged", false);
        out.putObject("serverInfo").put("name", "ai-learning-platform").put("version", "1.0.0");
        out.put("instructions", "Công cụ đọc và ghi bản nháp trên nền tảng học tập theo đúng quyền của tài khoản. "
                + "Không có công cụ nộp bài chính thức hay công bố điểm.");
        return out;
    }

    private ObjectNode toolsList(McpPrincipal principal) {
        ObjectNode out = json.createObjectNode();
        ArrayNode tools = out.putArray("tools");
        for (McpToolDefinition d : registry.visible(principal.actor().role(), principal.credential().access())) {
            ObjectNode tool = tools.addObject();
            tool.put("name", d.name());
            tool.put("description", d.description());
            tool.set("inputSchema", schemaNode(d.parameterSchema()));
            tool.putObject("annotations").put("readOnlyHint", d.kind() == McpToolKind.READ);
        }
        return out;
    }

    private ObjectNode toolsCall(McpPrincipal principal, JsonNode id, JsonNode params) {
        UUID accountId = principal.actor().accountId();
        UUID credentialId = principal.credential().id();
        JsonNode nameNode = params.path("name");
        if (!nameNode.isTextual() || nameNode.asText().isBlank() || nameNode.asText().length() > MAX_TOOL_NAME) {
            return error(id, INVALID_PARAMS, "Thiếu hoặc sai tên tool");
        }
        String name = nameNode.asText();
        boolean allowed;
        try {
            allowed = rateLimiter.tryAcquire(credentialId);
        } catch (RuntimeException e) {
            log.warn("Không kiểm được giới hạn lời gọi MCP: {}", e.toString());
            allowed = false;
        }
        if (!allowed) {
            record(accountId, name, AuditResult.DENIED, "RATE_LIMITED");
            return error(id, RATE_LIMITED, "Vượt 60 lời gọi mỗi phút; hãy thử lại sau");
        }
        Optional<McpToolRegistry.Entry> entry = registry.find(name, principal.actor().role(),
                principal.credential().access());
        if (entry.isEmpty()) {
            denied(accountId, name);
            return error(id, INVALID_PARAMS, "Không có tool này");
        }
        JsonNode args = params.has("arguments") ? params.get("arguments") : json.createObjectNode();
        if (!args.isObject()) {
            return error(id, INVALID_PARAMS, "arguments phải là object");
        }
        if (bytes(args.toString()) > MAX_ARGS_BYTES) {
            record(accountId, name, AuditResult.DENIED, "ARGS_TOO_LARGE");
            return error(id, INVALID_PARAMS, "Tham số vượt 64 KB");
        }
        Optional<String> invalid = schemas.check(entry.get().definition().parameterSchema(), args);
        if (invalid.isPresent()) {
            return error(id, INVALID_PARAMS, "Tham số không đúng schema: " + invalid.get());
        }
        Long inputTokens = reported(params.path("_meta").path("inputTokens"));
        Long outputTokens = reported(params.path("_meta").path("outputTokens"));

        McpToolResult result;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = json.convertValue(args, Map.class);
            result = "get_my_capabilities".equals(name)
                    ? new McpToolResult(false, Map.of("role", principal.actor().role(), "access", principal.credential().access(),
                            "tools", registry.visible(principal.actor().role(), principal.credential().access()).stream().map(McpToolDefinition::name).toList()))
                    : entry.get().provider().call(principal.actor(), name, map);
        } catch (ApiException e) {
            if (e.status().value() == 403 || e.status().value() == 404) {
                denied(accountId, name);
                usage.recordMcp(accountId, credentialId, name, "DENIED", inputTokens, outputTokens);
                return result(id, toolResult("Không tìm thấy hoặc không có quyền", true));
            }
            usage.recordMcp(accountId, credentialId, name, "ERROR", inputTokens, outputTokens);
            record(accountId, name, AuditResult.FAILURE, e.code());
            return result(id, toolResult(text(Map.of("code", e.code(), "message", e.getMessage())), true));
        } catch (RuntimeException e) {
            // chỉ ghi loại lỗi: thông điệp có thể chứa dữ liệu học vụ (SECURITY-03)
            log.warn("Tool MCP {} lỗi: {}", name, e.getClass().getName());
            usage.recordMcp(accountId, credentialId, name, "ERROR", inputTokens, outputTokens);
            record(accountId, name, AuditResult.FAILURE, "TOOL_ERROR");
            return result(id, toolResult("Tool gặp lỗi; hãy thử lại sau", true));
        }
        boolean isError = result == null || result.error();
        Object content = result == null ? null : result.content();
        String text = text(content);
        if (bytes(text) > 192 * 1024 || bytes(text(toolResult(text, isError))) > MAX_RESULT_BYTES - 512) {
            isError = true;
            text = "{\"code\":\"MCP_RESULT_TOO_LARGE\",\"complete\":false,\"message\":\"Reduce page size or use continuation\"}";
        }
        usage.recordMcp(accountId, credentialId, name, isError ? "ERROR" : "OK", inputTokens, outputTokens);
        record(accountId, name, isError ? AuditResult.FAILURE : AuditResult.SUCCESS, null);
        return result(id, toolResult(text, isError));
    }

    private ObjectNode toolResult(String text, boolean isError) {
        ObjectNode out = json.createObjectNode();
        out.putArray("content").addObject().put("type", "text").put("text", text);
        out.put("isError", isError);
        return out;
    }

    private String text(Object content) {
        if (content == null) {
            return "";
        }
        if (content instanceof String s) {
            return s;
        }
        try {
            return json.writeValueAsString(content);
        } catch (JsonProcessingException e) {
            return "";
        }
    }

    /** Cắt theo byte UTF-8, không cắt giữa một ký tự (F6 bước 7). */
    static String cut(String text) {
        byte[] raw = text.getBytes(StandardCharsets.UTF_8);
        if (raw.length <= MAX_RESULT_BYTES) {
            return text;
        }
        int limit = MAX_RESULT_BYTES - bytes(TRUNCATED);
        int end = 0;
        int used = 0;
        while (end < text.length()) {
            int cp = text.codePointAt(end);
            int size = cp < 0x80 ? 1 : cp < 0x800 ? 2 : cp < 0x10000 ? 3 : 4;
            if (used + size > limit) {
                break;
            }
            used += size;
            end += Character.charCount(cp);
        }
        return text.substring(0, end) + TRUNCATED;
    }

    private static int bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8).length;
    }

    /** Số token agent tự báo; không phải số nguyên không âm thì coi như không có (BR-U06-35). */
    private static Long reported(JsonNode node) {
        return node.isIntegralNumber() && node.asLong() >= 0 ? node.asLong() : null;
    }

    private JsonNode schemaNode(String schema) {
        try {
            return json.readTree(schema);
        } catch (JsonProcessingException | RuntimeException e) {
            return json.createObjectNode().put("type", "object");
        }
    }

    private void denied(UUID accountId, String tool) {
        audit.record(new AuditEvent(UUID.randomUUID(), accountId, "ACCESS_DENIED", "MCP_TOOL", tool,
                AuditResult.DENIED, "MCP", Map.of(), clock.instant()));
    }

    private void record(UUID accountId, String tool, AuditResult result, String reason) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("tool", tool);
        details.put("result", result.name());
        audit.record(new AuditEvent(UUID.randomUUID(), accountId, "MCP_TOOL_CALLED", "MCP_TOOL", tool, result, reason,
                details, clock.instant()));
    }

    private ObjectNode result(JsonNode id, JsonNode result) {
        ObjectNode out = envelope(id);
        out.set("result", result);
        return out;
    }

    ObjectNode error(JsonNode id, int code, String message) {
        ObjectNode out = envelope(id);
        out.putObject("error").put("code", code).put("message", message);
        return out;
    }

    private ObjectNode envelope(JsonNode id) {
        ObjectNode out = json.createObjectNode();
        out.put("jsonrpc", "2.0");
        if (id == null) {
            out.putNull("id");
        } else {
            out.set("id", id);
        }
        return out;
    }
}
