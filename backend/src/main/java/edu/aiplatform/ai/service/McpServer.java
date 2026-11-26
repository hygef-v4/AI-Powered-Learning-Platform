package edu.aiplatform.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.aiplatform.ai.service.McpCredentialService.McpPrincipal;
import edu.aiplatform.ai.contract.McpToolDefinition;
import edu.aiplatform.ai.contract.McpToolKind;
import edu.aiplatform.ai.contract.McpToolResult;
import edu.aiplatform.shared.error.ApiException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
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

    static final int PARSE_ERROR = -32700;
    static final int INVALID_REQUEST = -32600;
    static final int METHOD_NOT_FOUND = -32601;
    static final int INVALID_PARAMS = -32602;
    static final int RATE_LIMITED = -32029;
    static final int RATE_LIMIT_UNAVAILABLE = -32030;

    private static final Logger log = LoggerFactory.getLogger(McpServer.class);

    private final McpToolRegistry registry;
    private final IMcpRateLimiter rateLimiter;
    private final JsonSchemaChecker schemas;
    private final AiUsageService aiUsageService;
    private final ObjectMapper json;
    private final Clock clock;

    // Kết nối registry, giới hạn lượt gọi, kiểm schema và ghi nhận sử dụng cho máy chủ JSON-RPC.
    public McpServer(McpToolRegistry registry, IMcpRateLimiter rateLimiter, JsonSchemaChecker schemas,
                     AiUsageService aiUsageService, ObjectMapper json, Clock clock) {
        this.registry = registry;
        this.rateLimiter = rateLimiter;
        this.schemas = schemas;
        this.aiUsageService = aiUsageService;
        this.json = json;
        this.clock = clock;
    }

    // Kiểm phiên bản giao thức có trong tập phiên bản triển khai.
    public static boolean supports(String protocolVersion) {
        return PROTOCOLS.contains(protocolVersion);
    }

    /** Thân lời gọi không phải JSON. */
    public ObjectNode parseError() {
        return buildError(null, PARSE_ERROR, "Thân yêu cầu không phải JSON");
    }

    /**
     * Processes an incoming JSON-RPC 2.0 message according to MCP specification.
     */
    public Optional<ObjectNode> handleMessage(McpPrincipal principal, JsonNode message) {
        // Step 1: Validate root JSON-RPC structure and format
        if (message == null || !message.isObject()) {
            return Optional.of(buildError(null, INVALID_REQUEST, "Cần một thông điệp JSON-RPC"));
        }
        JsonNode id = message.get("id");
        JsonNode method = message.get("method");
        if (!"2.0".equals(message.path("jsonrpc").asText(null))) {
            return Optional.of(buildError(id, INVALID_REQUEST, "Cần jsonrpc 2.0"));
        }
        if (method == null) {
            return message.has("result") || message.has("error") ? Optional.empty()
                    : Optional.of(buildError(id, INVALID_REQUEST, "Thiếu method"));
        }
        if (!method.isTextual()) {
            return Optional.of(buildError(id, INVALID_REQUEST, "method phải là chuỗi"));
        }
        if (id == null) {
            return Optional.empty();
        }
        if (!(id.isTextual() || id.isIntegralNumber())) {
            return Optional.of(buildError(null, INVALID_REQUEST, "id phải là chuỗi hoặc số"));
        }

        // Step 2: Route request by method: initialize, ping, tools/list, or tools/call
        JsonNode params = message.path("params");
        return Optional.of(switch (method.asText()) {
            case "initialize" -> buildResult(id, initialize(params));
            case "ping" -> buildResult(id, json.createObjectNode());
            case "tools/list" -> buildResult(id, listTools(principal));
            case "tools/call" -> callTool(principal, id, params);
            default -> buildError(id, METHOD_NOT_FOUND, "Không hỗ trợ method này");
        });
    }

    /**
     * Handles MCP handshake initialization and announces supported server capabilities.
     */
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

    /**
     * Lists all tools accessible to the caller based on their role and credential access level.
     */
    private ObjectNode listTools(McpPrincipal principal) {
        ObjectNode out = json.createObjectNode();
        ArrayNode tools = out.putArray("tools");
        for (McpToolDefinition d : registry.listVisibleTools(principal.actor().role(), principal.credential().access())) {
            ObjectNode tool = tools.addObject();
            tool.put("name", d.name());
            tool.put("description", d.description());
            tool.set("inputSchema", parseSchemaNode(d.parameterSchema()));
            tool.putObject("annotations").put("readOnlyHint", d.kind() == McpToolKind.READ);
        }
        return out;
    }

    /**
     * Dispatches an individual tool call with rate limiting, schema verification, and auditing.
     */
    private ObjectNode callTool(McpPrincipal principal, JsonNode id, JsonNode params) {
        // Step 1: Validate tool name format and length constraints
        UUID accountId = principal.actor().accountId();
        UUID credentialId = principal.credential().id();
        JsonNode nameNode = params.path("name");
        if (!nameNode.isTextual() || nameNode.asText().isBlank() || nameNode.asText().length() > MAX_TOOL_NAME) {
            return buildError(id, INVALID_PARAMS, "Thiếu hoặc sai tên tool");
        }
        String name = nameNode.asText();

        // Step 2: Enforce per-credential rate limiting (maximum 60 calls per minute)
        boolean allowed;
        try {
            allowed = rateLimiter.tryAcquire(credentialId);
        } catch (RuntimeException e) {
            // Fail closed (BR-U06-53), but say why: an unreachable Redis is not the caller's fault.
            log.error("Không kiểm được giới hạn lời gọi MCP; từ chối lời gọi", e);
            return buildError(id, RATE_LIMIT_UNAVAILABLE,
                    "Hệ thống tạm không kiểm được giới hạn lời gọi; hãy thử lại sau");
        }
        if (!allowed) {
            return buildError(id, RATE_LIMITED, "Vượt 60 lời gọi mỗi phút; hãy thử lại sau");
        }

        // Step 3: Lookup tool definition in registry matching caller role and access permission
        Optional<McpToolRegistry.Entry> entry = registry.findTool(name, principal.actor().role(),
                principal.credential().access());
        if (entry.isEmpty()) {
            return buildError(id, INVALID_PARAMS, "Không có tool này");
        }

        // Step 4: Validate arguments object size (max 64 KB) and JSON schema compliance
        JsonNode args = params.has("arguments") ? params.get("arguments") : json.createObjectNode();
        if (!args.isObject()) {
            return buildError(id, INVALID_PARAMS, "arguments phải là object");
        }
        if (countBytes(args.toString()) > MAX_ARGS_BYTES) {
            return buildError(id, INVALID_PARAMS, "Tham số vượt 64 KB");
        }
        Optional<String> invalid = schemas.validateToolArguments(entry.get().definition().parameterSchema(), args);
        if (invalid.isPresent()) {
            // Reason code only: the validator message can echo agent-supplied property names.
            return buildError(id, INVALID_PARAMS, "Tham số không đúng schema: " + invalid.get());
        }
        Long inputTokens = readReportedTokens(params.path("_meta").path("inputTokens"));
        Long outputTokens = readReportedTokens(params.path("_meta").path("outputTokens"));

        // Step 5: Execute domain tool provider call and catch domain/runtime exceptions
        // Registry kiểm role và quyền token. Provider vẫn kiểm quyền trên lớp, môn hoặc bài cụ thể.
        McpToolResult result;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = json.convertValue(args, Map.class);
            result = "get_my_capabilities".equals(name)
                    ? new McpToolResult(false, Map.of("role", principal.actor().role(), "access", principal.credential().access(),
                            "tools", registry.listVisibleTools(principal.actor().role(), principal.credential().access()).stream().map(McpToolDefinition::name).toList(),
                            "workflow",McpCapabilitiesTools.describeWorkflow(principal.actor().role())))
                    : entry.get().provider().call(principal.actor(), name, map);
        } catch (ApiException e) {
            if (e.readStatus().value() == 403 || e.readStatus().value() == 404) {
                aiUsageService.recordMcp(accountId, credentialId, name, "DENIED", inputTokens, outputTokens);
                return buildResult(id, buildToolResult("Không tìm thấy hoặc không có quyền", true));
            }
            aiUsageService.recordMcp(accountId, credentialId, name, "ERROR", inputTokens, outputTokens);
            return buildResult(id, buildToolResult(renderText(Map.of("code", e.readCode(), "message", e.getMessage())), true));
        } catch (RuntimeException e) {
            log.warn("Tool MCP {} lỗi: {}", name, e.getClass().getName());
            aiUsageService.recordMcp(accountId, credentialId, name, "ERROR", inputTokens, outputTokens);
            return buildResult(id, buildToolResult("Tool gặp lỗi; hãy thử lại sau", true));
        }

        // Step 6: Verify result size limit (max 256 KB) and record usage
        boolean isError = result == null || result.error();
        Object content = result == null ? null : result.content();
        String text = renderText(content);
        if (countBytes(text) > 192 * 1024 || countBytes(renderText(buildToolResult(text, isError))) > MAX_RESULT_BYTES - 512) {
            isError = true;
            text = "{\"code\":\"MCP_RESULT_TOO_LARGE\",\"complete\":false,\"message\":\"Reduce page size or use continuation\"}";
        }
        aiUsageService.recordMcp(accountId, credentialId, name, isError ? "ERROR" : "OK", inputTokens, outputTokens);
        return buildResult(id, buildToolResult(text, isError));
    }

    // Đóng gói nội dung văn bản và cờ lỗi tool theo dạng phản hồi MCP.
    private ObjectNode buildToolResult(String text, boolean isError) {
        ObjectNode out = json.createObjectNode();
        out.putArray("content").addObject().put("type", "text").put("text", text);
        out.put("isError", isError);
        return out;
    }

    // Giữ nguyên chuỗi và tuần tự hóa dữ liệu cấu trúc sang văn bản JSON.
    private String renderText(Object content) {
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

    // Tính kích thước theo UTF-8 để kiểm giới hạn truyền dữ liệu.
    private static int countBytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8).length;
    }

    /** Số token agent tự báo; không phải số nguyên không âm thì coi như không có (BR-U06-35). */
    private static Long readReportedTokens(JsonNode node) {
        return node.isIntegralNumber() && node.asLong() >= 0 ? node.asLong() : null;
    }

    // Đọc schema của registry để liệt kê cho agent, dùng schema object tối thiểu nếu đọc lỗi.
    private JsonNode parseSchemaNode(String schema) {
        try {
            return json.readTree(schema);
        } catch (JsonProcessingException | RuntimeException e) {
            return json.createObjectNode().put("type", "object");
        }
    }

    // Bọc kết quả trong envelope JSON-RPC liên kết với ID yêu cầu.
    private ObjectNode buildResult(JsonNode id, JsonNode result) {
        ObjectNode out = createEnvelope(id);
        out.set("result", result);
        return out;
    }

    // Trả mã lỗi giao thức cùng ID để client ghép về đúng yêu cầu.
    ObjectNode buildError(JsonNode id, int code, String message) {
        ObjectNode out = createEnvelope(id);
        out.putObject("error").put("code", code).put("message", message);
        return out;
    }

    // Tạo phần chung của JSON-RPC 2.0, dùng ID null khi không có ID yêu cầu.
    private ObjectNode createEnvelope(JsonNode id) {
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
