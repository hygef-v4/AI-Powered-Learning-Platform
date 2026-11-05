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
    private final GradebookService service;
    private final GradebookExporter exporter;

    public GradebookController(GradebookService service, GradebookExporter exporter) {
        this.service = service;
        this.exporter = exporter;
    }

    @GetMapping("/gradebook")
    public GradebookPage list(@AuthenticationPrincipal Actor actor, @PathVariable UUID classId,
            @RequestParam(required = false) String q, @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.list(actor, classId, q, page, size);
    }

    @GetMapping("/gradebook/export")
    public void export(@AuthenticationPrincipal Actor actor, @PathVariable UUID classId,
            @RequestParam(required = false) UUID assignmentId, @RequestParam(defaultValue = "csv") String format,
            HttpServletResponse response) throws IOException {
        var rows = service.exportRows(actor, classId, assignmentId, format); // Complete authorization before
                                                                             // headers/bytes.
        response.setContentType(format.equals("csv") ? "text/csv; charset=UTF-8"
                : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Content-Disposition", "attachment; filename=\"gradebook-" + classId + "-"
                + LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")) + "." + format + "\"");
        exporter.write(rows, format, response.getOutputStream());
    }

    @GetMapping("/grade-distribution")
    public List<GradeDistributionCalculator.Bucket> distribution(@AuthenticationPrincipal Actor actor,
            @PathVariable UUID classId, @RequestParam UUID assignmentId) {
        return service.distribution(actor, classId, assignmentId, false);
    }

    @GetMapping("/students/{accountId}/grades")
    public GradebookRow student(@AuthenticationPrincipal Actor actor, @PathVariable UUID classId,
            @PathVariable UUID accountId) {
        return service.student(actor, classId, accountId);
    }
}
