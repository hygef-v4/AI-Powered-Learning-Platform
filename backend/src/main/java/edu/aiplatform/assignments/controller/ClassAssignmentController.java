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

/** UC 34, 35 phía lớp: bài của lớp trong tab Đánh giá, tạo bài, copy bài giữa lớp (F1, F3, F13). */
@RestController
@RequestMapping("/api/v1/classes/{classId}")
public class ClassAssignmentController {

    private final AssignmentAuthoringService assignmentAuthoringService;
    private final AssignmentCopyService assignmentCopyService;

    public ClassAssignmentController(AssignmentAuthoringService assignmentAuthoringService, AssignmentCopyService assignmentCopyService) {
        this.assignmentAuthoringService = assignmentAuthoringService;
        this.assignmentCopyService = assignmentCopyService;
    }

    @GetMapping("/assignments")
    public AssignmentViews.Page listAssignments(@AuthenticationPrincipal Actor actor, @PathVariable UUID classId,
                                     @RequestParam(required = false) Set<AssignmentStatus> status,
                                     @RequestParam(defaultValue = "false") boolean includeClosed,
                                     @RequestParam(required = false) String q,
                                     @RequestParam(required = false) GradingMode gradingMode) {
        return assignmentAuthoringService.listForClass(actor, classId, status, includeClosed, q, gradingMode);
    }

    @PostMapping(value = "/assignments", consumes = "application/json")
    public ResponseEntity<AssignmentViews.Detail> createAssignment(@AuthenticationPrincipal Actor actor,
                                                         @PathVariable UUID classId,
                                                         @RequestBody AssignmentRequests.Create body) {
        AssignmentViews.Detail created = assignmentAuthoringService.createForClass(actor, classId, body.buildCommand());
        return ResponseEntity.created(URI.create("/api/v1/assignments/" + created.id())).body(created);
    }

    /** Tài liệu tham khảo cho Tạo từ AI trong popup tạo bài, lúc bài chưa có. */
    @GetMapping("/assignment-materials")
    public List<AssignmentViews.Material> listMaterials(@AuthenticationPrincipal Actor actor, @PathVariable UUID classId) {
        return assignmentAuthoringService.listSelectableMaterialsForClass(actor, classId);
    }

    @GetMapping("/assignment-copy-sources")
    public List<AssignmentViews.CopySource> copySources(@AuthenticationPrincipal Actor actor,
                                                        @PathVariable UUID classId) {
        return assignmentCopyService.copySources(actor, classId);
    }

    @PostMapping(value = "/assignments:copy", consumes = "application/json")
    public ResponseEntity<AssignmentViews.Detail> copy(@AuthenticationPrincipal Actor actor, @PathVariable UUID classId,
                                                       @RequestBody AssignmentRequests.Copy body) {
        AssignmentViews.Detail created = assignmentCopyService.copyToClass(actor, classId, body.sourceAssignmentId(),
                body.title(), body.submissionMode(), body.gradingMode());
        return ResponseEntity.created(URI.create("/api/v1/assignments/" + created.id())).body(created);
    }
}
