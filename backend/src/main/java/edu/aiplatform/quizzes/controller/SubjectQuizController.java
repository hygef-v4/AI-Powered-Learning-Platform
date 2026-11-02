package edu.aiplatform.quizzes.controller;

import com.fasterxml.jackson.databind.JsonNode;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.quizzes.service.QuizAuthoringService;
import edu.aiplatform.quizzes.service.QuizViews.QuizDetail;
import edu.aiplatform.quizzes.service.QuizViews.QuizPage;
import edu.aiplatform.quizzes.contract.QuizStatus;
import java.net.URI;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** UC 48, F5, F6: Quiz List của môn (Chủ nhiệm môn). */
@RestController
@RequestMapping("/api/v1/subjects/{subjectId}/quizzes")
public class SubjectQuizController {

    private final QuizAuthoringService authoring;

    public SubjectQuizController(QuizAuthoringService authoring) {
        this.authoring = authoring;
    }

    @GetMapping
    public QuizPage list(@AuthenticationPrincipal Actor actor, @PathVariable UUID subjectId,
                         @RequestParam(required = false) QuizStatus status, @RequestParam(required = false) String q,
                         @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size,
                         @RequestParam MultiValueMap<String, String> params) {
        QuizRequests.query(params, Set.of("status", "q", "page", "size"));
        return authoring.listSubject(actor, subjectId, status, q, page, size);
    }

    @PostMapping(consumes = "application/json")
    public ResponseEntity<QuizDetail> create(@AuthenticationPrincipal Actor actor, @PathVariable UUID subjectId,
                                             @RequestBody JsonNode body) {
        QuizDetail created = authoring.createForSubject(actor, subjectId, QuizRequests.title(body));
        return ResponseEntity.created(URI.create("/api/v1/quizzes/" + created.quiz().id())).body(created);
    }
}
