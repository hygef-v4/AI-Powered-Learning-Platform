package edu.aiplatform.quizpractice.controller;

import com.fasterxml.jackson.databind.JsonNode;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.quizpractice.dto.PracticeViews.PracticeQuizDetail;
import edu.aiplatform.quizpractice.dto.PracticeViews.PracticeQuizRow;
import edu.aiplatform.quizpractice.dto.PracticeViews.ResultView;
import edu.aiplatform.quizpractice.dto.PracticeViews.SaveResult;
import edu.aiplatform.quizpractice.dto.PracticeViews.TakingView;
import edu.aiplatform.quizpractice.service.QuizActivityService;
import edu.aiplatform.quizpractice.service.QuizAttemptService;
import edu.aiplatform.quizpractice.service.QuizPracticeQueryService;
import edu.aiplatform.shared.error.ApiException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** UC 49–52: Quiz Practice của người học. Actor chỉ lấy từ phiên của U01. */
@RestController
@RequestMapping("/api/v1")
public class QuizPracticeController {

    private final QuizPracticeQueryService queries;
    private final QuizAttemptService attempts;
    private final QuizActivityService activity;

    public QuizPracticeController(QuizPracticeQueryService queries, QuizAttemptService attempts,
                                  QuizActivityService activity) {
        this.queries = queries;
        this.attempts = attempts;
        this.activity = activity;
    }

    /** {@code materialId} (cần kèm {@code classId}): Quiz gắn tài liệu cho Learning Material (G4). */
    @GetMapping("/me/quizzes")
    public List<PracticeQuizRow> history(@AuthenticationPrincipal Actor actor,
                                         @RequestParam(required = false) UUID classId,
                                         @RequestParam(required = false) UUID materialId,
                                         @RequestParam MultiValueMap<String, String> params) {
        PracticeRequests.query(params, Set.of("classId", "materialId"));
        if (materialId != null) {
            if (classId == null) {
                throw ApiException.badRequest("QUIZ_PRACTICE_REQUEST_INVALID", "Lọc theo tài liệu cần kèm classId");
            }
            return queries.byMaterial(actor, classId, materialId);
        }
        return queries.history(actor, classId);
    }

    @GetMapping("/me/quizzes/{quizId}")
    public PracticeQuizDetail detail(@AuthenticationPrincipal Actor actor, @PathVariable UUID quizId,
                                     @RequestParam UUID classId, @RequestParam MultiValueMap<String, String> params) {
        PracticeRequests.query(params, Set.of("classId"));
        return queries.detail(actor, quizId, classId);
    }

    @PostMapping(value = "/quizzes/{quizId}/attempts", consumes = "application/json")
    public TakingView start(@AuthenticationPrincipal Actor actor, @PathVariable UUID quizId, @RequestBody JsonNode body) {
        return attempts.start(actor, quizId, PracticeRequests.classId(body));
    }

    @GetMapping("/quiz-attempts/{attemptId}")
    public TakingView attempt(@AuthenticationPrincipal Actor actor, @PathVariable UUID attemptId) {
        return attempts.get(actor, attemptId);
    }

    @PutMapping(value = "/quiz-attempts/{attemptId}/answers", consumes = "application/json")
    public SaveResult save(@AuthenticationPrincipal Actor actor, @PathVariable UUID attemptId,
                           @RequestBody JsonNode body) {
        PracticeRequests.Answers request = PracticeRequests.answers(body);
        return attempts.saveAnswers(actor, attemptId, request.answers(), request.answerVersion());
    }

    @PostMapping("/quiz-attempts/{attemptId}/submit")
    public ResultView submit(@AuthenticationPrincipal Actor actor, @PathVariable UUID attemptId) {
        return attempts.submit(actor, attemptId);
    }

    @GetMapping("/quiz-attempts/{attemptId}/result")
    public ResultView result(@AuthenticationPrincipal Actor actor, @PathVariable UUID attemptId) {
        return attempts.result(actor, attemptId);
    }

    @PostMapping(value = "/quiz-attempts/{attemptId}/activity", consumes = "application/json")
    public ResponseEntity<Void> activity(@AuthenticationPrincipal Actor actor, @PathVariable UUID attemptId,
                                         @RequestBody JsonNode body) {
        activity.record(actor, attemptId, PracticeRequests.activity(body));
        return ResponseEntity.noContent().build();
    }
}
