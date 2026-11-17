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
import java.util.UUID;
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

    private final AiConnectionService aiConnectionService;
    private final ProviderCatalog catalog;
    private final AiSourceService aiSourceService;

    public AiConnectionController(AiConnectionService aiConnectionService, ProviderCatalog catalog, AiSourceService aiSourceService) {
        this.aiConnectionService = aiConnectionService;
        this.catalog = catalog;
        this.aiSourceService = aiSourceService;
    }

    /** {@code source} rỗng: hỏi mỗi lần. {@code provider} chỉ có với API cá nhân. */
    public record Preference(AiSource source, AiProvider provider) {

        static Preference mapPreference(Optional<AiSourcePreference> p) {
            return p.map(x -> new Preference(x.source(), x.provider())).orElse(new Preference(null, null));
        }
    }

    @GetMapping("/api/v1/me/ai-source-preference")
    public Preference loadPreference(@AuthenticationPrincipal Actor actor) {
        return Preference.mapPreference(aiSourceService.findPreference(actor));
    }

    @PutMapping("/api/v1/me/ai-source-preference")
    public Preference savePreference(@AuthenticationPrincipal Actor actor, @RequestBody Preference request) {
        return Preference.mapPreference(aiSourceService.savePreference(actor, request.source(), request.provider()));
    }

    @GetMapping("/api/v1/ai/providers")
    public List<ProviderInfo> listProviders() {
        return catalog.listProviders();
    }

    /** Khóa không được in ra log. */
    public record AddRequest(String provider, String apiKey, String model) {

        @Override
        public String toString() {
            return "AddRequest[provider=" + provider + ", model=" + model + "]";
        }
    }

    /** Khóa không được in ra log. */
    public record TestRequest(String provider, String apiKey, String model, UUID id) {

        @Override
        public String toString() {
            return "TestRequest[provider=" + provider + ", model=" + model + ", id=" + id + "]";
        }
    }

    public record StatusRequest(Boolean enabled, Long version) {
    }

    // Khóa API cá nhân: cùng kiểu với khóa của trường (yêu cầu 2026-11-05), nhiều khóa cho một nhà cung cấp, theo id.

    @GetMapping("/api/v1/me/ai-connections")
    public List<ConnectionView> listConnections(@AuthenticationPrincipal Actor actor) {
        return aiConnectionService.listPersonal(actor);
    }

    @PostMapping("/api/v1/me/ai-connections")
    public ConnectionView addConnection(@AuthenticationPrincipal Actor actor, @RequestBody AddRequest request) {
        return aiConnectionService.addPersonal(actor, AiWebSupport.parseProvider(request.provider()),
                new SaveCommand(request.apiKey(), request.model(), null));
    }

    @PostMapping("/api/v1/me/ai-connections:test")
    public AiConnectionService.TestResult testDraft(@AuthenticationPrincipal Actor actor, @RequestBody TestRequest request) {
        return aiConnectionService.testPersonalDraft(actor, AiWebSupport.parseProvider(request.provider()), request.id(),
                new SaveCommand(request.apiKey(), request.model(), null));
    }

    @PutMapping("/api/v1/me/ai-connections/{id}")
    public ConnectionView saveConnection(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                               @RequestBody SaveCommand command) {
        return aiConnectionService.replacePersonal(actor, id, command);
    }

    @PostMapping("/api/v1/me/ai-connections/{id}:test")
    public ConnectionView test(@AuthenticationPrincipal Actor actor, @PathVariable UUID id) {
        return aiConnectionService.testPersonal(actor, id);
    }

    @PutMapping("/api/v1/me/ai-connections/{id}/status")
    public ConnectionView updateConnectionStatus(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                 @RequestBody StatusRequest request) {
        return aiConnectionService.setPersonalEnabled(actor, id, Boolean.TRUE.equals(request.enabled()), request.version());
    }

    @DeleteMapping("/api/v1/me/ai-connections/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@AuthenticationPrincipal Actor actor, @PathVariable UUID id) {
        aiConnectionService.revokePersonal(actor, id);
    }
}
