package edu.aiplatform.grading.controller;

import edu.aiplatform.grading.service.GradebookExporter;
import edu.aiplatform.grading.service.GradebookService;
import edu.aiplatform.grading.dto.GradingViews.GradebookPage;
import edu.aiplatform.grading.dto.GradingViews.GradebookRow;
import edu.aiplatform.grading.model.GradeDistributionCalculator;
import edu.aiplatform.identity.contract.Actor;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/classes/{classId}")
public class GradebookController {
    private final GradebookService gradebookService;
    private final GradebookExporter gradebookExporter;

    /** Connects gradebook queries and file export to the HTTP endpoints. */
    public GradebookController(GradebookService gradebookService,
            GradebookExporter gradebookExporter) {
        this.gradebookService = gradebookService;
        this.gradebookExporter = gradebookExporter;
    }

    /** Returns a page of the class gradebook, optionally filtered by student name or email. */
    @GetMapping("/gradebook")
    public GradebookPage listGrades(@AuthenticationPrincipal Actor actor,
            @PathVariable UUID classId, @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return gradebookService.listGrades(actor, classId, q, page, size);
    }

    /**
     * Exports class gradebook data to a downloadable CSV or Excel (.xlsx) file.
     */
    @GetMapping("/gradebook/export")
    public void exportGrades(@AuthenticationPrincipal Actor actor, @PathVariable UUID classId,
            @RequestParam(required = false) UUID assignmentId,
            @RequestParam(defaultValue = "csv") String format, HttpServletResponse response)
            throws IOException {
        var rows = gradebookService.buildExportRows(actor, classId, assignmentId, format); // Complete authorization before
        // headers/bytes.
        response.setContentType(format.equals("csv") ? "text/csv; charset=UTF-8"
                : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Content-Disposition", "attachment; filename=\"gradebook-" + classId
                + "-" + LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")) + "." + format + "\"");
        gradebookExporter.writeGradebook(rows, format, response.getOutputStream());
    }

    /** Returns grade distribution buckets for the teacher's view of an assignment. */
    @GetMapping("/grade-distribution")
    public List<GradeDistributionCalculator.Bucket> loadDistribution(
            @AuthenticationPrincipal Actor actor, @PathVariable UUID classId,
            @RequestParam UUID assignmentId) {
        return gradebookService.loadDistribution(actor, classId, assignmentId, false);
    }

    /** Returns a student's gradebook row with current grades and submission status. */
    @GetMapping("/students/{accountId}/grades")
    public GradebookRow loadStudentGrades(@AuthenticationPrincipal Actor actor,
            @PathVariable UUID classId, @PathVariable UUID accountId) {
        return gradebookService.loadStudentGrades(actor, classId, accountId);
    }
}
