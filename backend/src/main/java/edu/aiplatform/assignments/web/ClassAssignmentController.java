package edu.aiplatform.assignments.web;

import edu.aiplatform.assignments.application.AssignmentAuthoringService;
import edu.aiplatform.assignments.application.AssignmentCopyService;
import edu.aiplatform.assignments.application.AssignmentViews;
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

/** UC 34, 35 phía lớp: bài của lớp trong tab Đánh giá, tạo bài, copy bài giữa lớp (F1, F3, F13). */
@RestController
@RequestMapping("/api/v1/classes/{classId}")
public class ClassAssignmentController {

    private final AssignmentAuthoringService authoring;
    private final AssignmentCopyService copies;

    public ClassAssignmentController(AssignmentAuthoringService authoring, AssignmentCopyService copies) {
        this.authoring = authoring;
        this.copies = copies;
    }

    @GetMapping("/assignments")
    public AssignmentViews.Page list(@AuthenticationPrincipal Actor actor, @PathVariable UUID classId,
                                     @RequestParam(required = false) Set<AssignmentStatus> status,
                                     @RequestParam(defaultValue = "false") boolean includeClosed,
                                     @RequestParam(required = false) String q,
                                     @RequestParam(required = false) GradingMode gradingMode) {
        return authoring.listForClass(actor, classId, status, includeClosed, q, gradingMode);
    }

    @PostMapping(value = "/assignments", consumes = "application/json")
    public ResponseEntity<AssignmentViews.Detail> create(@AuthenticationPrincipal Actor actor,
                                                         @PathVariable UUID classId,
                                                         @RequestBody AssignmentRequests.Create body) {
        AssignmentViews.Detail created = authoring.createForClass(actor, classId, body.command());
        return ResponseEntity.created(URI.create("/api/v1/assignments/" + created.id())).body(created);
    }

    @GetMapping("/assignment-copy-sources")
    public List<AssignmentViews.CopySource> copySources(@AuthenticationPrincipal Actor actor,
                                                        @PathVariable UUID classId) {
        return copies.copySources(actor, classId);
    }

    @PostMapping(value = "/assignments:copy", consumes = "application/json")
    public ResponseEntity<AssignmentViews.Detail> copy(@AuthenticationPrincipal Actor actor, @PathVariable UUID classId,
                                                       @RequestBody AssignmentRequests.Copy body) {
        AssignmentViews.Detail created = copies.copyToClass(actor, classId, body.sourceAssignmentId());
        return ResponseEntity.created(URI.create("/api/v1/assignments/" + created.id())).body(created);
    }
}
