package edu.aiplatform.quizzes.controller;

import com.fasterxml.jackson.databind.JsonNode;
import edu.aiplatform.ai.contract.SelectableMaterial;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.quizzes.service.AiDraftViews.AiDraftView;
import edu.aiplatform.quizzes.service.QuestionViews.QuestionPage;
import edu.aiplatform.quizzes.service.QuizAiDraftService;
import edu.aiplatform.quizzes.service.QuizAuthoringService;
import edu.aiplatform.quizzes.service.QuizCopyService;
import edu.aiplatform.quizzes.service.QuizPaperService;
import edu.aiplatform.quizzes.service.QuizPublicationService;
import edu.aiplatform.quizzes.service.QuizReferenceMaterials;
import edu.aiplatform.quizzes.service.QuizViews.BankTag;
import edu.aiplatform.quizzes.service.QuizViews.PaperPreview;
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

    private final QuizAuthoringService quizAuthoringService;
    private final QuizPublicationService quizPublicationService;
    private final QuizCopyService quizCopyService;
    private final QuizAiDraftService quizAiDraftService;
    private final QuizReferenceMaterials materials;
    private final QuizPaperService quizPaperService;

    public QuizController(QuizAuthoringService quizAuthoringService, QuizPublicationService quizPublicationService, QuizCopyService quizCopyService,
                          QuizAiDraftService quizAiDraftService, QuizReferenceMaterials materials, QuizPaperService quizPaperService) {
        this.quizAuthoringService = quizAuthoringService;
        this.quizPublicationService = quizPublicationService;
        this.quizCopyService = quizCopyService;
        this.quizAiDraftService = quizAiDraftService;
        this.materials = materials;
        this.quizPaperService = quizPaperService;
    }

    /** Tài liệu đưa vào popup AI của Quiz (H8). */
    @GetMapping("/reference-materials")
    public List<SelectableMaterial> listReferenceMaterials(@AuthenticationPrincipal Actor actor, @PathVariable UUID id) {
        return materials.listForQuiz(actor, id);
    }

    @GetMapping("/ai-drafts")
    public List<AiDraftView> listAiDrafts(@AuthenticationPrincipal Actor actor, @PathVariable UUID id) {
        return quizAiDraftService.listForQuiz(actor, id);
    }

    /** AI soạn câu cho Quiz (F8); trả đề xuất {@code PENDING} hoặc {@code FAILED} ngay khi U06 từ chối. */
    @PostMapping(value = "/ai-drafts", consumes = "application/json")
    public ResponseEntity<AiDraftView> requestAiDraft(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                                      @RequestBody JsonNode body) {
        AiDraftRequests.QuizDraft request = AiDraftRequests.parseQuizDraft(body);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(quizAiDraftService.requestForQuiz(actor, id, request.request(), request.version()));
    }

    @GetMapping("/ai-drafts/{draftId}")
    public AiDraftView loadAiDraft(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                               @PathVariable UUID draftId) {
        return quizAiDraftService.getForQuiz(actor, id, draftId);
    }

    @PostMapping(value = "/ai-drafts/{draftId}:accept", consumes = "application/json")
    public QuizDetail acceptAiDraft(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                    @PathVariable UUID draftId, @RequestBody JsonNode body) {
        AiDraftRequests.Accept accept = AiDraftRequests.accept(body, true);
        return quizAiDraftService.acceptForQuiz(actor, id, draftId, accept.questions(), accept.version());
    }

    @PostMapping("/ai-drafts/{draftId}:discard")
    public AiDraftView discardAiDraft(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                      @PathVariable UUID draftId) {
        return quizAiDraftService.discardForQuiz(actor, id, draftId);
    }

    /** {@code classId}: giảng viên mở Quiz của môn từ tab Đánh giá của lớp mình dạy (chỉ xem). */
    @GetMapping
    public QuizDetail loadQuiz(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                          @RequestParam(required = false) UUID classId,
                          @RequestParam MultiValueMap<String, String> params) {
        QuizRequests.query(params, Set.of("classId"));
        return quizAuthoringService.loadQuiz(actor, id, classId);
    }

    @PatchMapping(consumes = "application/json")
    public QuizDetail updateDetails(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                    @RequestBody JsonNode body) {
        QuizRequests.Details details = QuizRequests.parseDetails(body);
        return quizAuthoringService.updateDetails(actor, id, details.details(), details.version());
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteQuiz(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                       @RequestParam long version) {
        quizAuthoringService.deleteQuiz(actor, id, version);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/items", consumes = "application/json")
    public QuizDetail saveItems(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                @RequestBody JsonNode body) {
        QuizRequests.Items items = QuizRequests.parseItems(body);
        return quizAuthoringService.saveItems(actor, id, items.items(), items.version());
    }

    @PostMapping(value = "/items:random-preview", consumes = "application/json")
    public RandomPreview previewRandomQuestions(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                       @RequestBody JsonNode body) {
        QuizRequests.RandomPreview request = QuizRequests.parseRandomPreview(body);
        return quizAuthoringService.previewRandomQuestions(actor, id, request.count(), request.difficulty(), request.tag());
    }

    @GetMapping("/bank-questions")
    public QuestionPage listBankQuestions(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                      @RequestParam(required = false) Difficulty difficulty,
                                      @RequestParam(required = false) String tag,
                                      @RequestParam(required = false) String q,
                                      @RequestParam(required = false) Integer page,
                                      @RequestParam(required = false) Integer size,
                                      @RequestParam MultiValueMap<String, String> params) {
        QuizRequests.query(params, Set.of("difficulty", "tag", "q", "page", "size"));
        return quizAuthoringService.listBankQuestions(actor, id, difficulty, tag, q, page, size);
    }

    @PostMapping(value = "/inline-questions", consumes = "application/json")
    public QuizDetail addInline(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                @RequestBody JsonNode body) {
        QuestionRequests.Update request = QuestionRequests.parseUpdate(body);
        return quizAuthoringService.addInline(actor, id, request.content(), request.version());
    }

    @PatchMapping(value = "/inline-questions/{questionId}", consumes = "application/json")
    public QuizDetail updateInline(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                   @PathVariable UUID questionId, @RequestBody JsonNode body) {
        QuestionRequests.Update request = QuestionRequests.parseUpdate(body);
        return quizAuthoringService.updateInline(actor, id, questionId, request.content(), request.version());
    }

    @DeleteMapping("/inline-questions/{questionId}")
    public QuizDetail deleteInline(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                   @PathVariable UUID questionId, @RequestParam long version) {
        return quizAuthoringService.deleteInline(actor, id, questionId, version);
    }

    /** Lưu chế độ đề và quy tắc sinh đề (BR-U10-17). */
    @PutMapping(value = "/draw-rules", consumes = "application/json")
    public QuizDetail saveDrawRules(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                    @RequestBody JsonNode body) {
        QuizRequests.DrawRules request = QuizRequests.drawRules(body);
        return quizPaperService.saveRules(actor, id, request.paperMode(), request.rules(), request.version());
    }

    /** Thẻ trong ngân hàng của môn kèm số câu đang dùng. */
    @GetMapping("/bank-tags")
    public List<BankTag> listBankTags(@AuthenticationPrincipal Actor actor, @PathVariable UUID id) {
        return quizPaperService.listBankTags(actor, id);
    }

    /** Bốc thử một đề mẫu theo các quy tắc đã lưu; không lưu gì. */
    @PostMapping("/paper-preview")
    public PaperPreview previewPaper(@AuthenticationPrincipal Actor actor, @PathVariable UUID id) {
        return quizPaperService.preview(actor, id);
    }

    @PostMapping(value = "/publish", consumes = "application/json")
    public QuizDetail publish(@AuthenticationPrincipal Actor actor, @PathVariable UUID id, @RequestBody JsonNode body) {
        return quizPublicationService.publish(actor, id, QuestionRequests.readVersionOnly(body));
    }

    @PostMapping(value = "/retire", consumes = "application/json")
    public QuizDetail retire(@AuthenticationPrincipal Actor actor, @PathVariable UUID id, @RequestBody JsonNode body) {
        QuizRequests.Retire request = QuizRequests.retire(body);
        return quizPublicationService.retire(actor, id, request.reason(), request.version());
    }

    @PostMapping(value = "/reopen", consumes = "application/json")
    public QuizDetail reopen(@AuthenticationPrincipal Actor actor, @PathVariable UUID id, @RequestBody JsonNode body) {
        return quizPublicationService.reopen(actor, id, QuestionRequests.readVersionOnly(body));
    }

    @PostMapping("/clone")
    public ResponseEntity<QuizDetail> cloneQuiz(@AuthenticationPrincipal Actor actor, @PathVariable UUID id) {
        return respondCreated(quizCopyService.cloneQuiz(actor, id));
    }

    @PostMapping("/versions")
    public ResponseEntity<QuizDetail> createNewVersion(@AuthenticationPrincipal Actor actor, @PathVariable UUID id) {
        return respondCreated(quizCopyService.createNewVersion(actor, id));
    }

    private static ResponseEntity<QuizDetail> respondCreated(QuizDetail detail) {
        return ResponseEntity.created(URI.create("/api/v1/quizzes/" + detail.quiz().id())).body(detail);
    }
}
