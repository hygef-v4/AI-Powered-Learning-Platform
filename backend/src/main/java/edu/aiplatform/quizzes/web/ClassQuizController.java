package edu.aiplatform.quizzes.web;

import com.fasterxml.jackson.databind.JsonNode;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.quizzes.application.QuizAuthoringService;
import edu.aiplatform.quizzes.application.QuizCopyService;
import edu.aiplatform.quizzes.application.QuizViews.QuizDetail;
import edu.aiplatform.quizzes.application.QuizViews.QuizRow;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** UC 48, F5, F6, F11: phần Quiz trong tab Đánh giá của lớp (giảng viên lớp). */
@RestController
@RequestMapping("/api/v1/classes/{classId}")
public class ClassQuizController {

    private final QuizAuthoringService authoring;
    private final QuizCopyService copies;

    public ClassQuizController(QuizAuthoringService authoring, QuizCopyService copies) {
        this.authoring = authoring;
        this.copies = copies;
    }

    @GetMapping("/quizzes")
    public List<QuizRow> list(@AuthenticationPrincipal Actor actor, @PathVariable UUID classId) {
        return authoring.listClass(actor, classId);
    }

    @PostMapping(value = "/quizzes", consumes = "application/json")
    public ResponseEntity<QuizDetail> create(@AuthenticationPrincipal Actor actor, @PathVariable UUID classId,
                                             @RequestBody JsonNode body) {
        return created(authoring.createForClass(actor, classId, QuizRequests.title(body)));
    }

    @GetMapping("/quiz-copy-sources")
    public List<QuizRow> copySources(@AuthenticationPrincipal Actor actor, @PathVariable UUID classId) {
        return copies.copySources(actor, classId);
    }

    @PostMapping(value = "/quizzes:copy", consumes = "application/json")
    public ResponseEntity<QuizDetail> copy(@AuthenticationPrincipal Actor actor, @PathVariable UUID classId,
                                           @RequestBody JsonNode body) {
        return created(copies.copyToClass(actor, classId, QuizRequests.copySource(body)));
    }

    private static ResponseEntity<QuizDetail> created(QuizDetail detail) {
        return ResponseEntity.created(URI.create("/api/v1/quizzes/" + detail.quiz().id())).body(detail);
    }
}
