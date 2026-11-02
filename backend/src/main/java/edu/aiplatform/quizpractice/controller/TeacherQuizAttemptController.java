package edu.aiplatform.quizpractice.controller;

import com.fasterxml.jackson.databind.JsonNode;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.quizpractice.dto.PracticeViews.ClassAttempts;
import edu.aiplatform.quizpractice.dto.PracticeViews.TeacherAttemptDetail;
import edu.aiplatform.quizpractice.service.TeacherQuizAttemptService;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** UC 42 (phần Quiz): giảng viên của lớp xem lượt Quiz của sinh viên; chỉ sửa được quyết định trừ điểm. */
@RestController
@RequestMapping("/api/v1/classes/{classId}")
public class TeacherQuizAttemptController {

    private final TeacherQuizAttemptService teacher;

    public TeacherQuizAttemptController(TeacherQuizAttemptService teacher) {
        this.teacher = teacher;
    }

    @GetMapping("/quizzes/{quizId}/attempts")
    public ClassAttempts list(@AuthenticationPrincipal Actor actor, @PathVariable UUID classId,
                              @PathVariable UUID quizId) {
        return teacher.list(actor, classId, quizId);
    }

    /** Quyết định trừ hay không trừ điểm do rời trang, kèm lý do (BR-U11-33). */
    @PostMapping(value = "/quiz-attempts/{attemptId}/penalty:decide", consumes = "application/json")
    public TeacherAttemptDetail decidePenalty(@AuthenticationPrincipal Actor actor, @PathVariable UUID classId,
                                              @PathVariable UUID attemptId, @RequestBody JsonNode body) {
        PracticeRequests.PenaltyInput input = PracticeRequests.penaltyInput(body);
        return teacher.decidePenalty(actor, classId, attemptId, input.decision(), input.points(), input.reason());
    }

    @GetMapping("/quiz-attempts/{attemptId}")
    public TeacherAttemptDetail detail(@AuthenticationPrincipal Actor actor, @PathVariable UUID classId,
                                       @PathVariable UUID attemptId) {
        return teacher.detail(actor, classId, attemptId);
    }
}
