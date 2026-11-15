package edu.aiplatform.notifications.controller;

import edu.aiplatform.notifications.service.NotificationPreferenceService;
import edu.aiplatform.identity.contract.Actor;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me/notification-preferences")
public class NotificationPreferenceController {
    private final NotificationPreferenceService notificationPreferenceService;

    public NotificationPreferenceController(
            NotificationPreferenceService notificationPreferenceService) {
        this.notificationPreferenceService = notificationPreferenceService;
    }

    @GetMapping
    public Map<String, Boolean> loadPreferences(@AuthenticationPrincipal Actor actor) {
        return notificationPreferenceService.loadPreferences(actor);
    }

    @PutMapping
    public Map<String, Boolean> updatePreferences(
            @AuthenticationPrincipal Actor actor, @RequestBody Map<String, Boolean> input) {
        return notificationPreferenceService.updatePreferences(actor, input);
    }
}
