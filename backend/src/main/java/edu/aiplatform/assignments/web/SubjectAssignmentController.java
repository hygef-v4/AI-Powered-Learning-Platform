package edu.aiplatform.assignments.web;

import edu.aiplatform.assignments.application.AssignmentAuthoringService;
import edu.aiplatform.assignments.application.AssignmentViews;
import edu.aiplatform.assignments.contract.AssignmentStatus;
import edu.aiplatform.assignments.contract.GradingMode;
import edu.aiplatform.identity.contract.Actor;
import java.net.URI;
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

    public SubjectAssignmentController(AssignmentAuthoringService authoring) {
        this.authoring = authoring;
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
