package edu.aiplatform.grading.controller;

import edu.aiplatform.grading.service.GradebookService;
import edu.aiplatform.grading.dto.GradingViews.StudentGradeView;
import edu.aiplatform.grading.model.GradeDistributionCalculator;
import edu.aiplatform.identity.contract.Actor;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class MyGradeController {
    private final GradebookService gradebookService;

    public MyGradeController(GradebookService gradebookService) {
        this.gradebookService = gradebookService;
    }

    @GetMapping("/grades")
    public List<StudentGradeView> listMyGrades(@AuthenticationPrincipal Actor actor, @RequestParam UUID classId) {
        return gradebookService.listMyGrades(actor, classId);
    }

    @GetMapping("/grades/attempts/{attemptId}")
    public StudentGradeView loadAttemptGrade(@AuthenticationPrincipal Actor actor, @PathVariable UUID attemptId) {
        return gradebookService.loadAttemptGrade(actor, attemptId);
    }

    @GetMapping("/grade-distributions")
    public List<GradeDistributionCalculator.Bucket> loadGradeDistribution(@AuthenticationPrincipal Actor actor,
            @RequestParam UUID classId, @RequestParam UUID assignmentId) {
        return gradebookService.loadDistribution(actor, classId, assignmentId, true);
    }
}
