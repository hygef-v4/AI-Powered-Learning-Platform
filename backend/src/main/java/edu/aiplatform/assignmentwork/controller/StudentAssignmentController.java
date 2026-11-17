package edu.aiplatform.assignmentwork.controller;

import edu.aiplatform.assignmentwork.service.StudentAssignmentService;
import edu.aiplatform.assignmentwork.dto.WorkViews;
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

    private final StudentAssignmentService studentAssignmentService;

    public StudentAssignmentController(StudentAssignmentService studentAssignmentService) {
        this.studentAssignmentService = studentAssignmentService;
    }

    @GetMapping
    public List<WorkViews.Row> listAssignments(@AuthenticationPrincipal Actor actor,
                                    @RequestParam(required = false) UUID classId) {
        return studentAssignmentService.listAssignments(actor, classId);
    }

    /** Trang thông tin trước khi làm bài: không có đề (ảnh của khung chỉ mở trong lượt làm). */
    @GetMapping("/{assignmentId}")
    public WorkViews.Detail loadAssignment(@AuthenticationPrincipal Actor actor, @PathVariable UUID assignmentId,
                                   @RequestParam UUID classId) {
        return studentAssignmentService.loadAssignment(actor, assignmentId, classId);
    }
}
