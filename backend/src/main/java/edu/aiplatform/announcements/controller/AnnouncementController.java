package edu.aiplatform.announcements.controller;
import edu.aiplatform.announcements.dto.*;
import edu.aiplatform.announcements.service.*;
import edu.aiplatform.identity.contract.Actor;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1")
public class AnnouncementController {
    private final AnnouncementService service;
    public AnnouncementController(AnnouncementService service) {this.service=service;}
    @GetMapping("/classes/{classId}/announcements")
    public AnnouncementViews.Feed list(@AuthenticationPrincipal Actor actor,@PathVariable UUID classId,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size) {return service.list(actor,classId,page,size);}
    @PostMapping("/classes/{classId}/announcements") @ResponseStatus(HttpStatus.CREATED)
    public AnnouncementViews.Item create(@AuthenticationPrincipal Actor actor,@PathVariable UUID classId,@Valid @RequestBody AnnouncementCommands.Create input) {return service.create(actor,classId,input);}
    @PatchMapping("/announcements/{id}")
    public AnnouncementViews.Item edit(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@Valid @RequestBody AnnouncementCommands.Edit input) {return service.edit(actor,id,input);}
    @DeleteMapping("/announcements/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@RequestParam long version) {service.delete(actor,id,version);}
}
