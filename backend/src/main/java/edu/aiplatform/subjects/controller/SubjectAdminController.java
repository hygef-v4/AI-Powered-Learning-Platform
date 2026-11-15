package edu.aiplatform.subjects.controller;

import com.fasterxml.jackson.databind.JsonNode;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.subjects.service.SubjectAdminService;
import edu.aiplatform.subjects.dto.SubjectAdminViews;
import edu.aiplatform.subjects.service.SubjectCommandService;
import edu.aiplatform.subjects.service.SubjectStatusService;
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
    private final SubjectAdminService subjectAdminService;
    private final SubjectCommandService subjectCommandService;
    private final SubjectStatusService subjectStatusService;

    public SubjectAdminController(
            SubjectAdminService subjectAdminService,
            SubjectCommandService subjectCommandService,
            SubjectStatusService subjectStatusService) {
        this.subjectAdminService = subjectAdminService;
        this.subjectCommandService = subjectCommandService;
        this.subjectStatusService = subjectStatusService;
    }

    @GetMapping
    public SubjectAdminViews.Page listSubjects(
            @AuthenticationPrincipal Actor actor,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) SubjectStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam MultiValueMap<String, String> params) {
        SubjectRequests.validateListQuery(params);
        return subjectAdminService.listSubjects(actor, q, status, page, size);
    }

    @PostMapping(consumes = "application/json")
    public ResponseEntity<SubjectAdminViews.Detail> createSubject(
            @AuthenticationPrincipal Actor actor, @RequestBody JsonNode body) {
        var subject = subjectCommandService.createSubject(actor, SubjectRequests.parseCreate(body));
        return ResponseEntity.created(URI.create("/api/v1/admin/subjects/" + subject.id()))
                .body(SubjectResponses.mapSubjectDetail(subject));
    }

    @GetMapping("/{id}")
    public SubjectAdminViews.Detail loadSubject(
            @AuthenticationPrincipal Actor actor, @PathVariable UUID id) {
        return subjectAdminService.loadSubject(actor, id);
    }

    @GetMapping("/{id}/classes")
    public SubjectAdminViews.Classes listSubjectClasses(
            @AuthenticationPrincipal Actor actor, @PathVariable UUID id) {
        return subjectAdminService.listSubjectClasses(actor, id);
    }

    @PatchMapping(value = "/{id}", consumes = "application/json")
    public SubjectAdminViews.Detail updateSubjectInformation(
            @AuthenticationPrincipal Actor actor,
            @PathVariable UUID id,
            @RequestBody JsonNode body) {
        return SubjectResponses.mapSubjectDetail(
                subjectCommandService.updateInformation(
                        actor, id, SubjectRequests.parseUpdateInformation(body)));
    }

    @PutMapping(value = "/{id}/manager", consumes = "application/json")
    public SubjectAdminViews.Detail changeSubjectManager(
            @AuthenticationPrincipal Actor actor,
            @PathVariable UUID id,
            @RequestBody JsonNode body) {
        return SubjectResponses.mapSubjectDetail(
                subjectCommandService.changeManager(
                        actor, id, SubjectRequests.parseChangeManager(body)));
    }

    @PutMapping(value = "/{id}/status", consumes = "application/json")
    public SubjectAdminViews.Detail changeSubjectStatus(
            @AuthenticationPrincipal Actor actor,
            @PathVariable UUID id,
            @RequestBody JsonNode body) {
        return SubjectResponses.mapSubjectDetail(
                subjectStatusService.changeStatus(
                        actor, id, SubjectRequests.parseChangeStatus(body)));
    }
}
