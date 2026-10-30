package edu.aiplatform.settings.controller;

import com.fasterxml.jackson.databind.JsonNode;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.settings.service.SettingsService;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** UC 14 View Settings, UC 15 Edit Setting. */
@RestController
@RequestMapping("/api/v1/admin/settings")
@Validated
public class SettingsController {

    private final SettingsService settings;

    public SettingsController(SettingsService settings) {
        this.settings = settings;
    }

    public record UpdateRequest(JsonNode value, @NotNull Long version) {
    }

    @GetMapping
    public SettingsService.SettingsOverview list(@AuthenticationPrincipal Actor actor) {
        return settings.overview(actor);
    }

    @GetMapping("/{key}")
    public SettingsService.SettingItemView detail(@AuthenticationPrincipal Actor actor, @PathVariable String key) {
        return settings.detail(actor, key);
    }

    @PatchMapping("/{key}")
    public SettingsService.SettingItemView update(@AuthenticationPrincipal Actor actor, @PathVariable String key,
                                                  @RequestBody @Validated UpdateRequest request) {
        return settings.update(actor, key, request.value(), request.version());
    }
}
