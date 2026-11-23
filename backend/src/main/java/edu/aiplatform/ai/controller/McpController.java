package edu.aiplatform.ai.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.ai.service.McpCredentialService;
import edu.aiplatform.ai.service.McpCredentialService.McpPrincipal;
import edu.aiplatform.ai.service.McpServer;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * MCP server tại {@code /mcp}, transport Streamable HTTP ở chế độ trả JSON (BR-U06-32): mỗi {@code POST} một thông
 * điệp JSON-RPC, xác thực bằng {@code Authorization: Bearer} của credential MCP, không dùng cookie. Không mở luồng SSE
 * nên {@code GET} trả {@code 405}. Yêu cầu có header {@code Origin} (trình duyệt) bị từ chối để chặn DNS rebinding.
 */
@RestController
@RequestMapping("/mcp")
public class McpController {

    static final int MAX_BODY_BYTES = 1024 * 1024;
    static final String PROTOCOL_HEADER = "MCP-Protocol-Version";

    private final McpCredentialService mcpCredentialService;
    private final McpServer server;
    private final ObjectMapper json;

    /**
     * Initializes the controller with credentials, core server, and JSON mapper.
     */
    public McpController(McpCredentialService mcpCredentialService, McpServer server, ObjectMapper json) {
        this.mcpCredentialService = mcpCredentialService;
        this.server = server;
        this.json = json;
    }

    /**
     * Handles incoming MCP Streamable HTTP JSON-RPC messages from external agent clients.
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> post(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                  @RequestHeader(value = HttpHeaders.ORIGIN, required = false) String origin,
                                  @RequestHeader(value = PROTOCOL_HEADER, required = false) String protocol,
                                  HttpServletRequest request) throws IOException {
        // Step 1: Reject requests with Origin header to block browser-based DNS rebinding attacks
        if (origin != null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Step 2: Extract Bearer token and authenticate caller principal
        Optional<McpPrincipal> principal = extractBearer(authorization).flatMap(mcpCredentialService::authenticate);
        if (principal.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer realm=\"mcp\"").build();
        }

        // Step 3: Validate MCP protocol version if specified by caller
        if (protocol != null && !McpServer.supports(protocol)) {
            return ResponseEntity.badRequest().build();
        }

        // Step 4: Read at most 1 MB, and only after authentication, so anonymous callers cannot fill memory
        if (request.getContentLengthLong() > MAX_BODY_BYTES) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
        }
        byte[] raw = request.getInputStream().readNBytes(MAX_BODY_BYTES + 1);
        if (raw.length > MAX_BODY_BYTES) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
        }
        String body = raw.length == 0 ? null : new String(raw, StandardCharsets.UTF_8);

        // Step 5: Parse JSON body into JsonNode tree, catching parse errors
        JsonNode message;
        try {
            message = body == null ? null : json.readTree(body);
        } catch (JsonProcessingException e) {
            return ResponseEntity.ok(server.parseError());
        }

        // Step 6: Delegate message handling to McpServer and wrap output
        return server.handleMessage(principal.get(), message)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.accepted().build());
    }

    /**
     * Rejects unsupported HTTP methods (GET, DELETE) on the MCP endpoint.
     */
    @RequestMapping(method = {RequestMethod.GET, RequestMethod.DELETE})
    public ResponseEntity<Void> rejectUnsupported() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).header(HttpHeaders.ALLOW, "POST").build();
    }

    /**
     * Extracts the raw token string from the Authorization header if it has a Bearer prefix.
     */
    static Optional<String> extractBearer(String header) {
        if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return Optional.empty();
        }
        String token = header.substring(7).trim();
        return token.isEmpty() ? Optional.empty() : Optional.of(token);
    }
}
