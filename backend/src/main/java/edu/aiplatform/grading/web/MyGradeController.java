package edu.aiplatform.grading.web;

import edu.aiplatform.grading.application.GradebookService;
import edu.aiplatform.grading.application.GradingViews.StudentGradeView;
import edu.aiplatform.grading.domain.GradeDistributionCalculator;
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
    private final GradebookService grades;

    public MyGradeController(GradebookService grades) {
        this.grades = grades;
    }

    @GetMapping("/grades")
    public List<StudentGradeView> list(@AuthenticationPrincipal Actor actor, @RequestParam UUID classId) {
        return grades.myGrades(actor, classId);
    }

    @GetMapping("/grades/attempts/{attemptId}")
    public StudentGradeView attempt(@AuthenticationPrincipal Actor actor, @PathVariable UUID attemptId) {
        return grades.forAttempt(actor, attemptId);
    }

    @GetMapping("/grade-distributions")
    public List<GradeDistributionCalculator.Bucket> distributions(@AuthenticationPrincipal Actor actor,
            @RequestParam UUID classId, @RequestParam UUID assignmentId) {
        return grades.distribution(actor, classId, assignmentId, true);
    }
}
