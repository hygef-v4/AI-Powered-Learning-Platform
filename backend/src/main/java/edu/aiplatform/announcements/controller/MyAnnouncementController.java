package edu.aiplatform.announcements.controller;

import edu.aiplatform.announcements.dto.*;
import edu.aiplatform.announcements.service.*;
import edu.aiplatform.identity.contract.Actor;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me/announcements")
public class MyAnnouncementController {
    private final AnnouncementService announcementService;

    public MyAnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @GetMapping
    public AnnouncementViews.Feed listMyAnnouncements(
            @AuthenticationPrincipal Actor actor,
            @RequestParam(required = false) UUID classId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return announcementService.listAnnouncements(actor, classId, page, size);
    }
}
