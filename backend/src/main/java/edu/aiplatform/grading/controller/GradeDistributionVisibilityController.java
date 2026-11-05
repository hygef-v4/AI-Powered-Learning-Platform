package edu.aiplatform.grading.controller;

import edu.aiplatform.grading.service.GradeDistributionVisibilityService;
import edu.aiplatform.identity.contract.Actor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/classes/{classId}/assignments/{assignmentId}/grade-distribution-visibility")
public class GradeDistributionVisibilityController {
    public record Input(@NotNull Boolean enabled, @NotNull @Min(0) Long version) { }

    private final GradeDistributionVisibilityService gradeDistributionVisibilityService;

    public GradeDistributionVisibilityController(GradeDistributionVisibilityService gradeDistributionVisibilityService) {
        this.gradeDistributionVisibilityService = gradeDistributionVisibilityService;
    }

    @GetMapping
    public GradeDistributionVisibilityService.View loadVisibility(@AuthenticationPrincipal Actor actor,
            @PathVariable UUID classId, @PathVariable UUID assignmentId) {
        return gradeDistributionVisibilityService.loadVisibility(actor, classId, assignmentId);
    }

    @PutMapping
    public GradeDistributionVisibilityService.View updateVisibility(@AuthenticationPrincipal Actor actor,
            @PathVariable UUID classId, @PathVariable UUID assignmentId, @Valid @RequestBody Input input) {
        return gradeDistributionVisibilityService.updateVisibility(actor, classId, assignmentId, input.enabled(), input.version());
    }
}
