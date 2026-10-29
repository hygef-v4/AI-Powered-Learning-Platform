package edu.aiplatform.assignments.web;

import edu.aiplatform.assignments.application.AssignmentAiDraftService;
import edu.aiplatform.assignments.application.AssignmentViews;
import edu.aiplatform.identity.contract.Actor;
import java.util.UUID;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** AI soạn bản nháp (F6): gửi yêu cầu, xem đề xuất, áp dụng, bỏ. */
@RestController
@RequestMapping("/api/v1/assignments/{id}/ai-drafts")
public class AssignmentAiDraftController {

    private final AssignmentAiDraftService drafts;

    public AssignmentAiDraftController(AssignmentAiDraftService drafts) {
        this.drafts = drafts;
    }

    @PostMapping(consumes = "application/json")
    public AssignmentViews.AiDraft request(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                           @RequestBody AssignmentRequests.AiDraft body) {
        return drafts.request(actor, id, body.command());
    }

    @GetMapping("/{draftId}")
    public AssignmentViews.AiDraft get(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                       @PathVariable UUID draftId) {
        return drafts.get(actor, id, draftId);
    }

    @PostMapping(value = "/sources:preview", consumes = "application/json")
    public List<AssignmentViews.AiSourcePreview> previewSources(@AuthenticationPrincipal Actor actor,
                                                               @PathVariable UUID id,
                                                               @RequestBody AssignmentRequests.AiSources body) {
        return drafts.previewSources(actor, id, body.sourceFileRefs());
    }

    @PostMapping(value = "/{draftId}:apply", consumes = "application/json")
    public AssignmentViews.Detail apply(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                        @PathVariable UUID draftId, @RequestBody AssignmentRequests.Apply body) {
        return drafts.apply(actor, id, draftId, body.command());
    }

    @PostMapping("/{draftId}:discard")
    public AssignmentViews.AiDraft discard(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                           @PathVariable UUID draftId) {
        return drafts.discard(actor, id, draftId);
    }
}
