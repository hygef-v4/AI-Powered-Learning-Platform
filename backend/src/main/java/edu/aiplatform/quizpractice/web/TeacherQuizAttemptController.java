package edu.aiplatform.quizpractice.web;

import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.quizpractice.application.PracticeViews.ClassAttempts;
import edu.aiplatform.quizpractice.application.PracticeViews.TeacherAttemptDetail;
import edu.aiplatform.quizpractice.application.TeacherQuizAttemptService;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** UC 42 (phần Quiz): giảng viên của lớp xem lượt Quiz của sinh viên, chỉ đọc. */
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

    @GetMapping("/quiz-attempts/{attemptId}")
    public TeacherAttemptDetail detail(@AuthenticationPrincipal Actor actor, @PathVariable UUID classId,
                                       @PathVariable UUID attemptId) {
        return teacher.detail(actor, classId, attemptId);
    }
}
