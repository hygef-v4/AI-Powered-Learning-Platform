package edu.aiplatform.subjects.web;

import com.fasterxml.jackson.databind.JsonNode;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.subjects.application.SubjectAdminService;
import edu.aiplatform.subjects.application.SubjectAdminViews;
import edu.aiplatform.subjects.application.SubjectCommandService;
import edu.aiplatform.subjects.application.SubjectStatusService;
import edu.aiplatform.subjects.contract.SubjectStatus;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

/** UC16–19. Actor chỉ đến từ SecurityContext U01; không lấy accountId/role từ body/query/header tự chế. */
@RestController
@RequestMapping("/api/v1/admin/subjects")
public class SubjectAdminController {
    private final SubjectAdminService reads;
    private final SubjectCommandService commands;
    private final SubjectStatusService statuses;

    public SubjectAdminController(SubjectAdminService reads, SubjectCommandService commands, SubjectStatusService statuses) {
        this.reads = reads;
        this.commands = commands;
        this.statuses = statuses;
    }

    @GetMapping
    public SubjectAdminViews.Page list(@AuthenticationPrincipal Actor actor,
            @RequestParam(required = false) String q, @RequestParam(required = false) SubjectStatus status,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size,
            @RequestParam MultiValueMap<String, String> params) {
        SubjectRequests.query(params);
        return reads.list(actor, q, status, page, size);
    }

    @PostMapping(consumes = "application/json")
    public ResponseEntity<SubjectAdminViews.Detail> create(@AuthenticationPrincipal Actor actor, @RequestBody JsonNode body) {
        var subject = commands.create(actor, SubjectRequests.create(body));
        return ResponseEntity.created(URI.create("/api/v1/admin/subjects/" + subject.id())).body(SubjectResponses.detail(subject));
    }

    @GetMapping("/{id}")
    public SubjectAdminViews.Detail detail(@AuthenticationPrincipal Actor actor, @PathVariable UUID id) {
        return reads.detail(actor, id);
    }

    @GetMapping("/{id}/classes")
    public SubjectAdminViews.Classes classes(@AuthenticationPrincipal Actor actor, @PathVariable UUID id) {
        return reads.classes(actor, id);
    }

    @PatchMapping(value = "/{id}", consumes = "application/json")
    public SubjectAdminViews.Detail information(@AuthenticationPrincipal Actor actor, @PathVariable UUID id, @RequestBody JsonNode body) {
        return SubjectResponses.detail(commands.updateInformation(actor, id, SubjectRequests.information(body)));
    }

    @PutMapping(value = "/{id}/manager", consumes = "application/json")
    public SubjectAdminViews.Detail manager(@AuthenticationPrincipal Actor actor, @PathVariable UUID id, @RequestBody JsonNode body) {
        return SubjectResponses.detail(commands.changeManager(actor, id, SubjectRequests.manager(body)));
    }

    @PutMapping(value = "/{id}/status", consumes = "application/json")
    public SubjectAdminViews.Detail status(@AuthenticationPrincipal Actor actor, @PathVariable UUID id, @RequestBody JsonNode body) {
        return SubjectResponses.detail(statuses.changeStatus(actor, id, SubjectRequests.status(body)));
    }
}
