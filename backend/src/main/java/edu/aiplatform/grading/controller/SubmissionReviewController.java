package edu.aiplatform.grading.controller;

import edu.aiplatform.grading.dto.GradingViews.SubmissionPage;
import edu.aiplatform.grading.service.SubmissionReviewService;
import edu.aiplatform.identity.contract.Actor;
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
    private final SubmissionReviewService submissionReviewService;

    /** Creates the controller with review and gradebook services. */
    public SubmissionReviewController(SubmissionReviewService submissionReviewService) {
        this.submissionReviewService = submissionReviewService;
    }

    /**
     * Lists submissions for an assignment with optional status and late filters, supporting pagination.
     */
    @GetMapping("/assignments/{assignmentId}/submissions")
    public SubmissionPage listSubmissions(@AuthenticationPrincipal Actor actor,
            @PathVariable UUID classId, @PathVariable UUID assignmentId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean late,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return submissionReviewService.listSubmissions(actor, classId, assignmentId, status, late,
                page, size);
    }

    /**
     * Aggregates grading progress summaries for all gradable assignments in a course class.
     */
    @GetMapping("/submission-progress")
    public Map<UUID, SubmissionPage> progress(@AuthenticationPrincipal Actor actor,
            @PathVariable UUID classId) {
        return submissionReviewService.loadProgress(actor, classId);
    }
}
