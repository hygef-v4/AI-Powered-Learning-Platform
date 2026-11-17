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

    private final QuizPracticeQueryService quizPracticeQueryService;
    private final QuizAttemptService quizAttemptService;
    private final QuizActivityService quizActivityService;

    public QuizPracticeController(QuizPracticeQueryService quizPracticeQueryService, QuizAttemptService quizAttemptService,
                                  QuizActivityService quizActivityService) {
        this.quizPracticeQueryService = quizPracticeQueryService;
        this.quizAttemptService = quizAttemptService;
        this.quizActivityService = quizActivityService;
    }

    @GetMapping("/me/quizzes")
    public List<PracticeQuizRow> listHistory(@AuthenticationPrincipal Actor actor,
                                         @RequestParam(required = false) UUID classId,
                                         @RequestParam MultiValueMap<String, String> params) {
        PracticeRequests.query(params, Set.of("classId"));
        return quizPracticeQueryService.listHistory(actor, classId);
    }

    @GetMapping("/me/quizzes/{quizId}")
    public PracticeQuizDetail loadQuiz(@AuthenticationPrincipal Actor actor, @PathVariable UUID quizId,
                                     @RequestParam UUID classId, @RequestParam MultiValueMap<String, String> params) {
        PracticeRequests.query(params, Set.of("classId"));
        return quizPracticeQueryService.loadQuiz(actor, quizId, classId);
    }

    @PostMapping(value = "/quizzes/{quizId}/attempts", consumes = "application/json")
    public TakingView start(@AuthenticationPrincipal Actor actor, @PathVariable UUID quizId, @RequestBody JsonNode body) {
        return quizAttemptService.start(actor, quizId, PracticeRequests.parseClassId(body));
    }

    @GetMapping("/quiz-attempts/{attemptId}")
    public TakingView attempt(@AuthenticationPrincipal Actor actor, @PathVariable UUID attemptId) {
        return quizAttemptService.loadAttempt(actor, attemptId);
    }

    @PutMapping(value = "/quiz-attempts/{attemptId}/answers", consumes = "application/json")
    public SaveResult saveAnswers(@AuthenticationPrincipal Actor actor, @PathVariable UUID attemptId,
                           @RequestBody JsonNode body) {
        PracticeRequests.Answers request = PracticeRequests.parseAnswers(body);
        return quizAttemptService.saveAnswers(actor, attemptId, request.answers(), request.answerVersion());
    }

    @PostMapping("/quiz-attempts/{attemptId}/submit")
    public ResultView submit(@AuthenticationPrincipal Actor actor, @PathVariable UUID attemptId) {
        return quizAttemptService.submit(actor, attemptId);
    }

    @GetMapping("/quiz-attempts/{attemptId}/result")
    public ResultView loadResult(@AuthenticationPrincipal Actor actor, @PathVariable UUID attemptId) {
        return quizAttemptService.loadResult(actor, attemptId);
    }

    @PostMapping(value = "/quiz-attempts/{attemptId}/activity", consumes = "application/json")
    public ResponseEntity<Void> recordActivity(@AuthenticationPrincipal Actor actor, @PathVariable UUID attemptId,
                                         @RequestBody JsonNode body) {
        quizActivityService.record(actor, attemptId, PracticeRequests.parseActivity(body));
        return ResponseEntity.noContent().build();
    }
}
