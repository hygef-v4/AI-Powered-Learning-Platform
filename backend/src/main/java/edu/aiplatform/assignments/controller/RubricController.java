package edu.aiplatform.assignments.controller;

import edu.aiplatform.assignments.service.AssignmentAuthoringService;
import edu.aiplatform.assignments.dto.AssignmentViews;
import edu.aiplatform.identity.contract.Actor;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Rubric Detail (UC 36): xem, ghi đè rubric của một phần (F5). */
@RestController
@RequestMapping("/api/v1/assignments/{id}/parts/{partId}/rubric")
public class RubricController {

    private final AssignmentAuthoringService assignmentAuthoringService;

    public RubricController(AssignmentAuthoringService assignmentAuthoringService) {
        this.assignmentAuthoringService = assignmentAuthoringService;
    }

    @GetMapping
    public AssignmentViews.Rubric loadRubric(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                      @PathVariable String partId) {
        return assignmentAuthoringService.loadRubric(actor, id, partId);
    }

    @PutMapping(consumes = "application/json")
    public AssignmentViews.Rubric updateRubric(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                         @PathVariable String partId, @RequestBody AssignmentRequests.Rubric body) {
        return assignmentAuthoringService.updateRubric(actor, id, partId, body.buildCommand());
    }
}
