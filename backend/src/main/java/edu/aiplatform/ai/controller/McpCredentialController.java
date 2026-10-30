package edu.aiplatform.ai.controller;

import edu.aiplatform.ai.service.McpCredentialService;
import edu.aiplatform.ai.service.McpCredentialService.Created;
import edu.aiplatform.ai.service.McpCredentialService.CredentialView;
import edu.aiplatform.ai.model.McpCredential.Access;
import edu.aiplatform.identity.contract.Actor;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** UC 32: credential MCP của chính người dùng; token rõ có trong phản hồi tạo và khi chủ agent xem lại. */
@RestController
@RequestMapping("/api/v1/me/mcp-credentials")
public class McpCredentialController {

    private final McpCredentialService credentials;

    public McpCredentialController(McpCredentialService credentials) {
        this.credentials = credentials;
    }

    public record CreateRequest(String label, Access access, Integer days) {
    }

    public record UpdateRequest(String label, Access access) {
    }

    /** Token rõ, chỉ trả cho chủ agent; không in ra log. */
    public record TokenResponse(String token) {

        @Override
        public String toString() {
            return "TokenResponse[***]";
        }
    }

    /** {@code endpoint} là đường dẫn MCP trên cùng địa chỉ backend; frontend ghép với origin để hướng dẫn. */
    public record CreateResponse(CredentialView credential, String token, String endpoint) {

        @Override
        public String toString() {
            return "CreateResponse[credential=" + credential + "]";
        }
    }

    @GetMapping
    public List<CredentialView> list(@AuthenticationPrincipal Actor actor) {
        return credentials.list(actor);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateResponse create(@AuthenticationPrincipal Actor actor, @RequestBody CreateRequest request) {
        Created created = credentials.create(actor, request.label(), request.access(), request.days());
        return new CreateResponse(created.credential(), created.token(), "/mcp");
    }

    @PatchMapping("/{id}")
    public CredentialView update(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                 @RequestBody UpdateRequest request) {
        return credentials.update(actor, id, request.label(), request.access());
    }

    @GetMapping("/{id}/token")
    public TokenResponse token(@AuthenticationPrincipal Actor actor, @PathVariable UUID id) {
        return new TokenResponse(credentials.reveal(actor, id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@AuthenticationPrincipal Actor actor, @PathVariable UUID id) {
        credentials.revoke(actor, id);
    }
}
