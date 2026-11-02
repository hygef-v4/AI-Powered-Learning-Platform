package edu.aiplatform.ai.web;

import edu.aiplatform.ai.application.AiConnectionService;
import edu.aiplatform.ai.application.AiConnectionService.ConnectionView;
import edu.aiplatform.ai.application.AiConnectionService.SaveCommand;
import edu.aiplatform.ai.application.ProviderCatalog;
import edu.aiplatform.ai.application.ProviderCatalog.ProviderInfo;
import edu.aiplatform.identity.contract.Actor;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** UC 31: API cá nhân của Student, Teacher, Subject Manager và danh sách provider, model cho phép. */
@RestController
public class AiConnectionController {

    private final AiConnectionService connections;
    private final ProviderCatalog catalog;

    public AiConnectionController(AiConnectionService connections, ProviderCatalog catalog) {
        this.connections = connections;
        this.catalog = catalog;
    }

    @GetMapping("/api/v1/ai/providers")
    public List<ProviderInfo> providers() {
        return catalog.providers();
    }

    @GetMapping("/api/v1/me/ai-connections")
    public List<ConnectionView> list(@AuthenticationPrincipal Actor actor) {
        return connections.listPersonal(actor);
    }

    @PutMapping("/api/v1/me/ai-connections/{provider}")
    public ConnectionView save(@AuthenticationPrincipal Actor actor, @PathVariable String provider,
                               @RequestBody SaveCommand command) {
        return connections.savePersonal(actor, AiWebSupport.provider(provider), command);
    }

    @PostMapping("/api/v1/me/ai-connections/{provider}:test")
    public ConnectionView test(@AuthenticationPrincipal Actor actor, @PathVariable String provider) {
        return connections.testPersonal(actor, AiWebSupport.provider(provider));
    }

    @DeleteMapping("/api/v1/me/ai-connections/{provider}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@AuthenticationPrincipal Actor actor, @PathVariable String provider) {
        connections.revokePersonal(actor, AiWebSupport.provider(provider));
    }
}
