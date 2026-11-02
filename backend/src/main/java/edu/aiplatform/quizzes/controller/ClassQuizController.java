package edu.aiplatform.quizzes.controller;

import com.fasterxml.jackson.databind.JsonNode;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.quizzes.service.QuestionProposalService;
import edu.aiplatform.quizzes.service.QuestionViews.ProposalView;
import edu.aiplatform.quizzes.service.QuizAuthoringService;
import edu.aiplatform.quizzes.service.QuizCopyService;
import edu.aiplatform.quizzes.service.QuizViews.QuizDetail;
import edu.aiplatform.quizzes.service.QuizViews.QuizRow;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** UC 48, F5, F6, F11: phần Quiz trong tab Đánh giá của lớp (giảng viên lớp). */
@RestController
@RequestMapping("/api/v1/classes/{classId}")
public class ClassQuizController {

    private final QuizAuthoringService authoring;
    private final QuizCopyService copies;
    private final QuestionProposalService proposals;

    public ClassQuizController(QuizAuthoringService authoring, QuizCopyService copies,
                               QuestionProposalService proposals) {
        this.authoring = authoring;
        this.copies = copies;
        this.proposals = proposals;
    }

    /** Câu mình đã đề xuất vào ngân hàng của môn từ lớp này (BR-U10-09). */
    @GetMapping("/question-proposals")
    public List<ProposalView> proposals(@AuthenticationPrincipal Actor actor, @PathVariable UUID classId) {
        return proposals.list(actor, classId);
    }

    @PostMapping(value = "/question-proposals", consumes = "application/json")
    public ResponseEntity<ProposalView> propose(@AuthenticationPrincipal Actor actor, @PathVariable UUID classId,
                                                @RequestBody JsonNode body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(proposals.propose(actor, classId, QuestionRequests.create(body)));
    }

    /** Sửa đề xuất đang chờ hoặc đã bị từ chối; lưu là gửi lại để duyệt. */
    @PatchMapping(value = "/question-proposals/{id}", consumes = "application/json")
    public ProposalView resubmit(@AuthenticationPrincipal Actor actor, @PathVariable UUID classId,
                                 @PathVariable UUID id, @RequestBody JsonNode body) {
        QuestionRequests.Update update = QuestionRequests.update(body);
        return proposals.resubmit(actor, classId, id, update.content(), update.version());
    }

    @DeleteMapping("/question-proposals/{id}")
    public ResponseEntity<Void> withdraw(@AuthenticationPrincipal Actor actor, @PathVariable UUID classId,
                                         @PathVariable UUID id, @RequestParam long version) {
        proposals.withdraw(actor, classId, id, version);
        return ResponseEntity.noContent().build();
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
