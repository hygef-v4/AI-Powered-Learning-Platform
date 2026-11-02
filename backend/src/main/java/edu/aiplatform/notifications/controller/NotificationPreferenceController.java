package edu.aiplatform.notifications.controller;
import edu.aiplatform.notifications.service.NotificationPreferenceService;
import edu.aiplatform.identity.contract.Actor;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/me/notification-preferences")
public class NotificationPreferenceController {
    private final NotificationPreferenceService service;
    public NotificationPreferenceController(NotificationPreferenceService service) {this.service=service;}
    @GetMapping public Map<String,Boolean> get(@AuthenticationPrincipal Actor actor) {return service.get(actor);}
    @PutMapping public Map<String,Boolean> put(@AuthenticationPrincipal Actor actor,@RequestBody Map<String,Boolean> input) {return service.put(actor,input);}
}
