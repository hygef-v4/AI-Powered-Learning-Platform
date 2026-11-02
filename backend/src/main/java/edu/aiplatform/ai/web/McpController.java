package edu.aiplatform.ai.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.ai.application.McpCredentialService;
import edu.aiplatform.ai.application.McpCredentialService.McpPrincipal;
import edu.aiplatform.ai.application.McpServer;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    static final long MAX_BODY_BYTES = 1024 * 1024;
    static final String PROTOCOL_HEADER = "MCP-Protocol-Version";

    private final McpCredentialService credentials;
    private final McpServer server;
    private final ObjectMapper json;

    public McpController(McpCredentialService credentials, McpServer server, ObjectMapper json) {
        this.credentials = credentials;
        this.server = server;
        this.json = json;
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> post(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                  @RequestHeader(value = HttpHeaders.ORIGIN, required = false) String origin,
                                  @RequestHeader(value = PROTOCOL_HEADER, required = false) String protocol,
                                  @RequestHeader(value = HttpHeaders.CONTENT_LENGTH, required = false) Long length,
                                  @RequestBody(required = false) String body) {
        if (origin != null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Optional<McpPrincipal> principal = bearer(authorization).flatMap(credentials::authenticate);
        if (principal.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer realm=\"mcp\"").build();
        }
        if (protocol != null && !McpServer.supports(protocol)) {
            return ResponseEntity.badRequest().build();
        }
        if ((length != null && length > MAX_BODY_BYTES) || (body != null && body.length() > MAX_BODY_BYTES)) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
        }
        JsonNode message;
        try {
            message = body == null ? null : json.readTree(body);
        } catch (JsonProcessingException e) {
            return ResponseEntity.ok(server.parseError());
        }
        return server.handle(principal.get(), message)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.accepted().build());
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.DELETE})
    public ResponseEntity<Void> notSupported() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).header(HttpHeaders.ALLOW, "POST").build();
    }

    static Optional<String> bearer(String header) {
        if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return Optional.empty();
        }
        String token = header.substring(7).trim();
        return token.isEmpty() ? Optional.empty() : Optional.of(token);
    }
}
