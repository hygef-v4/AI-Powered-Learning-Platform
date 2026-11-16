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

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public record UpdateRequest(JsonNode value, @NotNull Long version) {
    }

    @GetMapping
    public SettingsService.SettingsOverview loadOverview(@AuthenticationPrincipal Actor actor) {
        return settingsService.loadOverview(actor);
    }

    @GetMapping("/{key}")
    public SettingsService.SettingItemView loadSetting(@AuthenticationPrincipal Actor actor, @PathVariable String key) {
        return settingsService.loadSetting(actor, key);
    }

    @PatchMapping("/{key}")
    public SettingsService.SettingItemView updateSetting(@AuthenticationPrincipal Actor actor, @PathVariable String key,
                                                  @RequestBody @Validated UpdateRequest request) {
        return settingsService.updateSetting(actor, key, request.value(), request.version());
    }
}
