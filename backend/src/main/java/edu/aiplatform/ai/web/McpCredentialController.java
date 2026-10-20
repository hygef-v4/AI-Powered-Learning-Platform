package edu.aiplatform.ai.web;

import edu.aiplatform.ai.application.McpCredentialService;
import edu.aiplatform.ai.application.McpCredentialService.Created;
import edu.aiplatform.ai.application.McpCredentialService.CredentialView;
import edu.aiplatform.ai.domain.McpCredential.Access;
import edu.aiplatform.identity.contract.Actor;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** UC 32: credential MCP của chính người dùng; token rõ chỉ có trong phản hồi tạo (BR-U06-31). */
@RestController
@RequestMapping("/api/v1/me/mcp-credentials")
public class McpCredentialController {

    private final McpCredentialService credentials;

    public McpCredentialController(McpCredentialService credentials) {
        this.credentials = credentials;
    }

    public record CreateRequest(String label, Access access, Integer days) {
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

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@AuthenticationPrincipal Actor actor, @PathVariable UUID id) {
        credentials.revoke(actor, id);
    }
}
