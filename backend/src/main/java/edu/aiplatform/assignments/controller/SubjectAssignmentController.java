package edu.aiplatform.assignments.controller;

import edu.aiplatform.assignments.service.AssignmentAuthoringService;
import edu.aiplatform.assignments.service.AssignmentCopyService;
import edu.aiplatform.assignments.dto.AssignmentViews;
import edu.aiplatform.assignments.contract.AssignmentStatus;
import edu.aiplatform.assignments.contract.GradingMode;
import edu.aiplatform.identity.contract.Actor;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** UC 34, 35 phía môn: Assignment List của Chủ nhiệm môn (F2, F3). */
@RestController
@RequestMapping("/api/v1/subjects/{subjectId}/assignments")
public class SubjectAssignmentController {

    private final AssignmentAuthoringService authoring;
    private final AssignmentCopyService copies;

    public SubjectAssignmentController(AssignmentAuthoringService authoring, AssignmentCopyService copies) {
        this.authoring = authoring;
        this.copies = copies;
    }

    /** Bài để copy vào bài của môn: bài khác của cùng môn và bài của các lớp mình dạy. */
    @GetMapping("/copy-sources")
    public List<AssignmentViews.CopySource> copySources(@AuthenticationPrincipal Actor actor, @PathVariable UUID subjectId) {
        return copies.subjectCopySources(actor, subjectId);
    }

    @PostMapping(value = "/copies", consumes = "application/json")
    public ResponseEntity<AssignmentViews.Detail> copy(@AuthenticationPrincipal Actor actor, @PathVariable UUID subjectId,
                                                       @RequestBody AssignmentRequests.Copy body) {
        AssignmentViews.Detail created = copies.copyToSubject(actor, subjectId, body.sourceAssignmentId());
        return ResponseEntity.created(URI.create("/api/v1/assignments/" + created.id())).body(created);
    }

    @GetMapping
    public AssignmentViews.Page list(@AuthenticationPrincipal Actor actor, @PathVariable UUID subjectId,
                                     @RequestParam(required = false) Set<AssignmentStatus> status,
                                     @RequestParam(defaultValue = "false") boolean includeClosed,
                                     @RequestParam(required = false) String q,
                                     @RequestParam(required = false) GradingMode gradingMode) {
        return authoring.listForSubject(actor, subjectId, status, includeClosed, q, gradingMode);
    }

    @PostMapping(consumes = "application/json")
    public ResponseEntity<AssignmentViews.Detail> create(@AuthenticationPrincipal Actor actor,
                                                         @PathVariable UUID subjectId,
                                                         @RequestBody AssignmentRequests.Create body) {
        AssignmentViews.Detail created = authoring.createForSubject(actor, subjectId, body.command());
        return ResponseEntity.created(URI.create("/api/v1/assignments/" + created.id())).body(created);
    }
}
