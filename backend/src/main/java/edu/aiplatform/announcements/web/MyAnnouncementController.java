package edu.aiplatform.announcements.web;
import edu.aiplatform.announcements.application.*;
import edu.aiplatform.identity.contract.Actor;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/me/announcements")
public class MyAnnouncementController {
    private final AnnouncementService service;
    public MyAnnouncementController(AnnouncementService service) {this.service=service;}
    @GetMapping
    public AnnouncementViews.Feed list(@AuthenticationPrincipal Actor actor,@RequestParam(required=false) UUID classId,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size) {return service.list(actor,classId,page,size);}
}
