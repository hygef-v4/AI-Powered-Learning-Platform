package edu.aiplatform.notifications.controller;
import edu.aiplatform.notifications.service.NotificationService;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.groupdocs.contract.ISseHub;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
@RestController @RequestMapping("/api/v1/me")
public class NotificationController {
    private final NotificationService service;private final ISseHub sse;
    public NotificationController(NotificationService service,ISseHub sse) {this.service=service;this.sse=sse;}
    @GetMapping("/notifications")
    public NotificationService.Feed list(@AuthenticationPrincipal Actor actor,@RequestParam(defaultValue="false") boolean unread,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size) {return service.list(actor,unread,page,size);}
    @GetMapping("/notifications/unread-count")
    public Map<String,Long> unread(@AuthenticationPrincipal Actor actor) {return Map.of("count",service.unread(actor));}
    @PostMapping("/notifications/{id}:read") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void read(@AuthenticationPrincipal Actor actor,@PathVariable UUID id) {service.read(actor,id);}
    @PostMapping("/notifications:read-all") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void readAll(@AuthenticationPrincipal Actor actor) {service.readAll(actor);}
    @GetMapping(value="/notifications/events",produces=MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> events(@AuthenticationPrincipal Actor actor,jakarta.servlet.http.HttpServletResponse response) {
        String channel=service.authorizedChannel(actor);
        response.setHeader("X-Accel-Buffering","no");
        response.setHeader("Cache-Control","no-store");
        return ResponseEntity.ok().header("X-Accel-Buffering","no").cacheControl(CacheControl.noStore()).body(sse.subscribe(channel));
    }
}
