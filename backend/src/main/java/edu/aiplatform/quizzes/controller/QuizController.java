package edu.aiplatform.quizzes.controller;

import com.fasterxml.jackson.databind.JsonNode;
import edu.aiplatform.ai.contract.SelectableMaterial;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.quizzes.service.AiDraftViews.AiDraftView;
import edu.aiplatform.quizzes.service.QuestionViews.QuestionPage;
import edu.aiplatform.quizzes.service.QuizAiDraftService;
import edu.aiplatform.quizzes.service.QuizAuthoringService;
import edu.aiplatform.quizzes.service.QuizCopyService;
import edu.aiplatform.quizzes.service.QuizPublicationService;
import edu.aiplatform.quizzes.service.QuizReferenceMaterials;
import edu.aiplatform.quizzes.service.QuizViews.QuizDetail;
import edu.aiplatform.quizzes.service.QuizViews.RandomPreview;
import edu.aiplatform.quizzes.model.Difficulty;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** UC 48: Quiz Detail (F7, F9–F12). Actor chỉ lấy từ phiên của U01. */
@RestController
@RequestMapping("/api/v1/quizzes/{id}")
public class QuizController {

    private final QuizAuthoringService authoring;
    private final QuizPublicationService publication;
    private final QuizCopyService copies;
    private final QuizAiDraftService drafts;
    private final QuizReferenceMaterials materials;

    public QuizController(QuizAuthoringService authoring, QuizPublicationService publication, QuizCopyService copies,
                          QuizAiDraftService drafts, QuizReferenceMaterials materials) {
        this.authoring = authoring;
        this.publication = publication;
        this.copies = copies;
        this.drafts = drafts;
        this.materials = materials;
    }

    /** Tài liệu gắn được vào Quiz hoặc đưa vào popup AI (H8). */
    @GetMapping("/reference-materials")
    public List<SelectableMaterial> referenceMaterials(@AuthenticationPrincipal Actor actor, @PathVariable UUID id) {
        return materials.forQuiz(actor, id);
    }

    @GetMapping("/ai-drafts")
    public List<AiDraftView> aiDrafts(@AuthenticationPrincipal Actor actor, @PathVariable UUID id) {
        return drafts.listForQuiz(actor, id);
    }

    /** AI soạn câu cho Quiz (F8); trả đề xuất {@code PENDING} hoặc {@code FAILED} ngay khi U06 từ chối. */
    @PostMapping(value = "/ai-drafts", consumes = "application/json")
    public ResponseEntity<AiDraftView> requestAiDraft(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                                      @RequestBody JsonNode body) {
        AiDraftRequests.QuizDraft request = AiDraftRequests.quiz(body);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(drafts.requestForQuiz(actor, id, request.request(), request.version()));
    }

    @GetMapping("/ai-drafts/{draftId}")
    public AiDraftView aiDraft(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                               @PathVariable UUID draftId) {
        return drafts.getForQuiz(actor, id, draftId);
    }

    @PostMapping(value = "/ai-drafts/{draftId}:accept", consumes = "application/json")
    public QuizDetail acceptAiDraft(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                    @PathVariable UUID draftId, @RequestBody JsonNode body) {
        AiDraftRequests.Accept accept = AiDraftRequests.accept(body, true);
        return drafts.acceptForQuiz(actor, id, draftId, accept.questions(), accept.version());
    }

    @PostMapping("/ai-drafts/{draftId}:discard")
    public AiDraftView discardAiDraft(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                      @PathVariable UUID draftId) {
        return drafts.discardForQuiz(actor, id, draftId);
    }

    /** {@code classId}: giảng viên mở Quiz của môn từ tab Đánh giá của lớp mình dạy (chỉ xem). */
    @GetMapping
    public QuizDetail get(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                          @RequestParam(required = false) UUID classId,
                          @RequestParam MultiValueMap<String, String> params) {
        QuizRequests.query(params, Set.of("classId"));
        return authoring.get(actor, id, classId);
    }

    @PatchMapping(consumes = "application/json")
    public QuizDetail updateDetails(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                    @RequestBody JsonNode body) {
        QuizRequests.Details details = QuizRequests.details(body);
        return authoring.updateDetails(actor, id, details.details(), details.version());
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                       @RequestParam long version) {
        authoring.delete(actor, id, version);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/items", consumes = "application/json")
    public QuizDetail saveItems(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                @RequestBody JsonNode body) {
        QuizRequests.Items items = QuizRequests.items(body);
        return authoring.saveItems(actor, id, items.items(), items.version());
    }

    @PostMapping(value = "/items:random-preview", consumes = "application/json")
    public RandomPreview randomPreview(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                       @RequestBody JsonNode body) {
        QuizRequests.RandomPreview request = QuizRequests.randomPreview(body);
        return authoring.randomPreview(actor, id, request.count(), request.difficulty(), request.tag());
    }

    @GetMapping("/bank-questions")
    public QuestionPage bankQuestions(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                      @RequestParam(required = false) Difficulty difficulty,
                                      @RequestParam(required = false) String tag,
                                      @RequestParam(required = false) String q,
                                      @RequestParam(required = false) Integer page,
                                      @RequestParam(required = false) Integer size,
                                      @RequestParam MultiValueMap<String, String> params) {
        QuizRequests.query(params, Set.of("difficulty", "tag", "q", "page", "size"));
        return authoring.bankQuestions(actor, id, difficulty, tag, q, page, size);
    }

    @PostMapping(value = "/inline-questions", consumes = "application/json")
    public QuizDetail addInline(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                @RequestBody JsonNode body) {
        QuestionRequests.Update request = QuestionRequests.update(body);
        return authoring.addInline(actor, id, request.content(), request.version());
    }

    @PatchMapping(value = "/inline-questions/{questionId}", consumes = "application/json")
    public QuizDetail updateInline(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                   @PathVariable UUID questionId, @RequestBody JsonNode body) {
        QuestionRequests.Update request = QuestionRequests.update(body);
        return authoring.updateInline(actor, id, questionId, request.content(), request.version());
    }

    @DeleteMapping("/inline-questions/{questionId}")
    public QuizDetail deleteInline(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                   @PathVariable UUID questionId, @RequestParam long version) {
        return authoring.deleteInline(actor, id, questionId, version);
    }

    @PostMapping(value = "/review", consumes = "application/json")
    public QuizDetail review(@AuthenticationPrincipal Actor actor, @PathVariable UUID id, @RequestBody JsonNode body) {
        return publication.review(actor, id, QuestionRequests.versionOnly(body));
    }

    @PostMapping(value = "/publish", consumes = "application/json")
    public QuizDetail publish(@AuthenticationPrincipal Actor actor, @PathVariable UUID id, @RequestBody JsonNode body) {
        return publication.publish(actor, id, QuestionRequests.versionOnly(body));
    }

    @PostMapping(value = "/retire", consumes = "application/json")
    public QuizDetail retire(@AuthenticationPrincipal Actor actor, @PathVariable UUID id, @RequestBody JsonNode body) {
        QuizRequests.Retire request = QuizRequests.retire(body);
        return publication.retire(actor, id, request.reason(), request.version());
    }

    @PostMapping(value = "/reopen", consumes = "application/json")
    public QuizDetail reopen(@AuthenticationPrincipal Actor actor, @PathVariable UUID id, @RequestBody JsonNode body) {
        return publication.reopen(actor, id, QuestionRequests.versionOnly(body));
    }

    @PostMapping("/clone")
    public ResponseEntity<QuizDetail> cloneQuiz(@AuthenticationPrincipal Actor actor, @PathVariable UUID id) {
        return created(copies.cloneQuiz(actor, id));
    }

    @PostMapping("/versions")
    public ResponseEntity<QuizDetail> newVersion(@AuthenticationPrincipal Actor actor, @PathVariable UUID id) {
        return created(copies.newVersion(actor, id));
    }

    private static ResponseEntity<QuizDetail> created(QuizDetail detail) {
        return ResponseEntity.created(URI.create("/api/v1/quizzes/" + detail.quiz().id())).body(detail);
    }
}
