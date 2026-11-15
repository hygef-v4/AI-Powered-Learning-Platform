package edu.aiplatform.notifications.controller;

import edu.aiplatform.notifications.service.NotificationService;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.groupdocs.contract.ISseHub;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/me")
public class NotificationController {
    private final NotificationService notificationService;
    private final ISseHub sseHub;

    public NotificationController(NotificationService notificationService, ISseHub sseHub) {
        this.notificationService = notificationService;
        this.sseHub = sseHub;
    }

    @GetMapping("/notifications")
    public NotificationService.Feed listNotifications(
            @AuthenticationPrincipal Actor actor,
            @RequestParam(defaultValue = "false") boolean unread,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return notificationService.listNotifications(actor, unread, page, size);
    }

    @GetMapping("/notifications/unread-count")
    public Map<String, Long> countUnreadNotifications(@AuthenticationPrincipal Actor actor) {
        return Map.of("count", notificationService.countUnread(actor));
    }

    @PostMapping("/notifications/{id}:read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markNotificationRead(@AuthenticationPrincipal Actor actor, @PathVariable UUID id) {
        notificationService.markRead(actor, id);
    }

    @PostMapping("/notifications:read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllNotificationsRead(@AuthenticationPrincipal Actor actor) {
        notificationService.markAllRead(actor);
    }

    @GetMapping(value = "/notifications/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> streamNotificationEvents(
            @AuthenticationPrincipal Actor actor,
            jakarta.servlet.http.HttpServletResponse response) {
        String channel = notificationService.resolveAuthorizedChannel(actor);
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-store");
        return ResponseEntity.ok()
                .header("X-Accel-Buffering", "no")
                .cacheControl(CacheControl.noStore())
                .body(sseHub.subscribe(channel));
    }
}
