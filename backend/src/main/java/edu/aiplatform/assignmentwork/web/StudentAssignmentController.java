package edu.aiplatform.assignmentwork.web;

import edu.aiplatform.assignmentwork.application.StudentAssignmentService;
import edu.aiplatform.assignmentwork.application.WorkViews;
import edu.aiplatform.identity.contract.Actor;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Student Assignments, Assignment Detail (UC 37, 38). */
@RestController
@RequestMapping("/api/v1/me/assignments")
public class StudentAssignmentController {

    private final StudentAssignmentService assignments;

    public StudentAssignmentController(StudentAssignmentService assignments) {
        this.assignments = assignments;
    }

    @GetMapping
    public List<WorkViews.Row> list(@AuthenticationPrincipal Actor actor,
                                    @RequestParam(required = false) UUID classId) {
        return assignments.list(actor, classId);
    }

    /** Trang thông tin trước khi làm bài: không có đề (ảnh của khung chỉ mở trong lượt làm). */
    @GetMapping("/{assignmentId}")
    public WorkViews.Detail detail(@AuthenticationPrincipal Actor actor, @PathVariable UUID assignmentId,
                                   @RequestParam UUID classId) {
        return assignments.detail(actor, assignmentId, classId);
    }
}
