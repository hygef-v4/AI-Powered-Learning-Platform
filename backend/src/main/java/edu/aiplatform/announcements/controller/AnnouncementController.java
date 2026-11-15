package edu.aiplatform.announcements.controller;

import edu.aiplatform.announcements.dto.*;
import edu.aiplatform.announcements.service.*;
import edu.aiplatform.identity.contract.Actor;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class AnnouncementController {
    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @GetMapping("/classes/{classId}/announcements")
    public AnnouncementViews.Feed listClassAnnouncements(
            @AuthenticationPrincipal Actor actor,
            @PathVariable UUID classId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return announcementService.listAnnouncements(actor, classId, page, size);
    }

    @PostMapping("/classes/{classId}/announcements")
    @ResponseStatus(HttpStatus.CREATED)
    public AnnouncementViews.Item createAnnouncement(
            @AuthenticationPrincipal Actor actor,
            @PathVariable UUID classId,
            @Valid @RequestBody AnnouncementCommands.Create input) {
        return announcementService.createAnnouncement(actor, classId, input);
    }

    @PatchMapping("/announcements/{id}")
    public AnnouncementViews.Item editAnnouncement(
            @AuthenticationPrincipal Actor actor,
            @PathVariable UUID id,
            @Valid @RequestBody AnnouncementCommands.Edit input) {
        return announcementService.editAnnouncement(actor, id, input);
    }

    @DeleteMapping("/announcements/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAnnouncement(
            @AuthenticationPrincipal Actor actor,
            @PathVariable UUID id,
            @RequestParam long version) {
        announcementService.deleteAnnouncement(actor, id, version);
    }
}
