package edu.aiplatform.quizzes.controller;

import com.fasterxml.jackson.databind.JsonNode;
import edu.aiplatform.ai.contract.SelectableMaterial;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.quizzes.service.AiDraftViews.AiDraftView;
import edu.aiplatform.quizzes.service.AiDraftViews.BankAcceptResult;
import edu.aiplatform.quizzes.service.QuestionBankService;
import edu.aiplatform.quizzes.service.QuestionImporter;
import edu.aiplatform.quizzes.service.QuestionViews.QuestionDetail;
import edu.aiplatform.quizzes.service.QuestionViews.QuestionPage;
import edu.aiplatform.quizzes.service.QuizAiDraftService;
import edu.aiplatform.quizzes.service.QuizReferenceMaterials;
import edu.aiplatform.quizzes.contract.QuestionType;
import edu.aiplatform.quizzes.model.Difficulty;
import edu.aiplatform.quizzes.model.QuestionStatus;
import edu.aiplatform.shared.error.ApiException;
import java.io.IOException;
import java.net.URI;
import java.util.List;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** UC 46, 47: ngân hàng câu hỏi của môn. Actor chỉ lấy từ phiên của U01, không lấy từ body/query. */
@RestController
@RequestMapping("/api/v1")
public class QuestionController {

    private final QuestionBankService bank;
    private final QuestionImporter importer;
    private final QuizAiDraftService drafts;
    private final QuizReferenceMaterials materials;

    public QuestionController(QuestionBankService bank, QuestionImporter importer, QuizAiDraftService drafts,
                              QuizReferenceMaterials materials) {
        this.bank = bank;
        this.importer = importer;
        this.drafts = drafts;
        this.materials = materials;
    }

    /** Tài liệu của môn cho popup AI của ngân hàng (H8). */
    @GetMapping("/subjects/{subjectId}/reference-materials")
    public List<SelectableMaterial> referenceMaterials(@AuthenticationPrincipal Actor actor,
                                                       @PathVariable UUID subjectId) {
        return materials.forSubject(actor, subjectId);
    }

    @GetMapping("/subjects/{subjectId}/question-ai-drafts")
    public List<AiDraftView> aiDrafts(@AuthenticationPrincipal Actor actor, @PathVariable UUID subjectId) {
        return drafts.listForBank(actor, subjectId);
    }

    /** AI soạn câu cho ngân hàng (F4). */
    @PostMapping(value = "/subjects/{subjectId}/question-ai-drafts", consumes = "application/json")
    public ResponseEntity<AiDraftView> requestAiDraft(@AuthenticationPrincipal Actor actor,
                                                      @PathVariable UUID subjectId, @RequestBody JsonNode body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(drafts.requestForBank(actor, subjectId, AiDraftRequests.bank(body)));
    }

    @GetMapping("/subjects/{subjectId}/question-ai-drafts/{draftId}")
    public AiDraftView aiDraft(@AuthenticationPrincipal Actor actor, @PathVariable UUID subjectId,
                               @PathVariable UUID draftId) {
        return drafts.getForBank(actor, subjectId, draftId);
    }

    @PostMapping(value = "/subjects/{subjectId}/question-ai-drafts/{draftId}:accept", consumes = "application/json")
    public BankAcceptResult acceptAiDraft(@AuthenticationPrincipal Actor actor, @PathVariable UUID subjectId,
                                          @PathVariable UUID draftId, @RequestBody JsonNode body) {
        return drafts.acceptForBank(actor, subjectId, draftId, AiDraftRequests.accept(body, false).questions());
    }

    @PostMapping("/subjects/{subjectId}/question-ai-drafts/{draftId}:discard")
    public AiDraftView discardAiDraft(@AuthenticationPrincipal Actor actor, @PathVariable UUID subjectId,
                                      @PathVariable UUID draftId) {
        return drafts.discardForBank(actor, subjectId, draftId);
    }

    @GetMapping("/subjects/{subjectId}/questions")
    public QuestionPage list(@AuthenticationPrincipal Actor actor, @PathVariable UUID subjectId,
                             @RequestParam(required = false) QuestionType type,
                             @RequestParam(required = false) Difficulty difficulty,
                             @RequestParam(required = false) QuestionStatus status,
                             @RequestParam(required = false) String q,
                             @RequestParam(required = false) Integer page,
                             @RequestParam(required = false) Integer size,
                             @RequestParam MultiValueMap<String, String> params) {
        QuestionRequests.query(params);
        return bank.list(actor, subjectId, new QuestionBankService.Filter(type, difficulty, status, q, page, size));
    }

    @PostMapping(value = "/subjects/{subjectId}/questions", consumes = "application/json")
    public ResponseEntity<QuestionDetail> create(@AuthenticationPrincipal Actor actor, @PathVariable UUID subjectId,
                                                 @RequestBody JsonNode body) {
        QuestionDetail created = bank.create(actor, subjectId, QuestionRequests.create(body));
        return ResponseEntity.created(URI.create("/api/v1/questions/" + created.question().id())).body(created);
    }

    @PostMapping(value = "/subjects/{subjectId}/questions:import", consumes = "multipart/form-data")
    public QuestionImporter.ImportResult importQuestions(@AuthenticationPrincipal Actor actor,
                                                         @PathVariable UUID subjectId,
                                                         @RequestPart("file") MultipartFile file,
                                                         @RequestParam(defaultValue = "false") boolean dryRun) {
        if (file.getSize() > QuestionImporter.MAX_BYTES) {
            throw ApiException.payloadTooLarge("IMPORT_FILE_TOO_LARGE", "Tệp tối đa 5 MB");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException unreadable) {
            throw ApiException.badRequest("IMPORT_FILE_INVALID", "Không đọc được tệp");
        }
        return importer.importFile(actor, subjectId, bytes, dryRun);
    }

    @GetMapping("/questions/{id}")
    public QuestionDetail detail(@AuthenticationPrincipal Actor actor, @PathVariable UUID id) {
        return bank.detail(actor, id);
    }

    @PatchMapping(value = "/questions/{id}", consumes = "application/json")
    public QuestionDetail update(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                 @RequestBody JsonNode body) {
        QuestionRequests.Update update = QuestionRequests.update(body);
        return bank.update(actor, id, update.content(), update.version());
    }

    @DeleteMapping("/questions/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                       @RequestParam long version) {
        bank.delete(actor, id, version);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/questions/{id}/versions")
    public QuestionDetail openDraft(@AuthenticationPrincipal Actor actor, @PathVariable UUID id) {
        return bank.openDraft(actor, id);
    }

    @PostMapping(value = "/questions/{id}/activate", consumes = "application/json")
    public QuestionDetail activate(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                   @RequestBody JsonNode body) {
        return bank.activate(actor, id, QuestionRequests.versionOnly(body));
    }

    @PostMapping(value = "/questions/{id}/retire", consumes = "application/json")
    public QuestionDetail retire(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                 @RequestBody JsonNode body) {
        return bank.retire(actor, id, QuestionRequests.versionOnly(body));
    }
}
