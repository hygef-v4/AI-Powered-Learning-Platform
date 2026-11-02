package edu.aiplatform.ai.controller;

import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.ai.contract.AiSource;
import edu.aiplatform.ai.model.AiSourcePreference;
import edu.aiplatform.ai.service.AiConnectionService;
import edu.aiplatform.ai.service.AiConnectionService.ConnectionView;
import edu.aiplatform.ai.service.AiConnectionService.SaveCommand;
import edu.aiplatform.ai.service.AiSourceService;
import edu.aiplatform.ai.service.ProviderCatalog;
import edu.aiplatform.ai.service.ProviderCatalog.ProviderInfo;
import edu.aiplatform.identity.contract.Actor;
import java.util.List;
import java.util.Optional;
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

/**
 * UC 31: API cá nhân của Student, Teacher, Subject Manager, danh sách provider, model cho phép và nguồn AI mặc định
 * (FB-U06-01).
 */
@RestController
public class AiConnectionController {

    private final AiConnectionService connections;
    private final ProviderCatalog catalog;
    private final AiSourceService sources;

    public AiConnectionController(AiConnectionService connections, ProviderCatalog catalog, AiSourceService sources) {
        this.connections = connections;
        this.catalog = catalog;
        this.sources = sources;
    }

    /** {@code source} rỗng: hỏi mỗi lần. {@code provider} chỉ có với API cá nhân. */
    public record Preference(AiSource source, AiProvider provider) {

        static Preference of(Optional<AiSourcePreference> p) {
            return p.map(x -> new Preference(x.source(), x.provider())).orElse(new Preference(null, null));
        }
    }

    @GetMapping("/api/v1/me/ai-source-preference")
    public Preference preference(@AuthenticationPrincipal Actor actor) {
        return Preference.of(sources.preference(actor));
    }

    @PutMapping("/api/v1/me/ai-source-preference")
    public Preference savePreference(@AuthenticationPrincipal Actor actor, @RequestBody Preference request) {
        return Preference.of(sources.savePreference(actor, request.source(), request.provider()));
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
