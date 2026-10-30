package edu.aiplatform.grading.controller;

import edu.aiplatform.assignments.contract.GradingMode;
import edu.aiplatform.assignments.contract.IAssignmentQuery;
import edu.aiplatform.grading.service.GradebookService;
import edu.aiplatform.grading.dto.GradingViews.SubmissionPage;
import edu.aiplatform.grading.service.SubmissionReviewService;
import edu.aiplatform.identity.contract.Actor;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/classes/{classId}")
public class SubmissionReviewController {
    private final SubmissionReviewService review;
    private final IAssignmentQuery assignments;
    private final GradebookService gradebook;

    public SubmissionReviewController(SubmissionReviewService review, IAssignmentQuery assignments,
            GradebookService gradebook) {
        this.review = review;
        this.assignments = assignments;
        this.gradebook = gradebook;
    }

    @GetMapping("/assignments/{assignmentId}/submissions")
    public SubmissionPage submissions(@AuthenticationPrincipal Actor actor, @PathVariable UUID classId,
            @PathVariable UUID assignmentId, @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean late, @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return review.list(actor, classId, assignmentId, status, late, page, size);
    }

    @GetMapping("/submission-progress")
    public Map<UUID, SubmissionPage> progress(@AuthenticationPrincipal Actor actor, @PathVariable UUID classId) {
        gradebook.list(actor, classId, null, 1, 1);
        var result = new LinkedHashMap<UUID, SubmissionPage>();
        for (var a : assignments.listForClass(classId))
            if (a.gradingMode() == GradingMode.GRADED)
                result.put(a.assignmentId(), review.list(actor, classId, a.assignmentId(), null, null, 1, 1));
        return result;
    }
}
