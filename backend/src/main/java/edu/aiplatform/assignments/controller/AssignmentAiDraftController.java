package edu.aiplatform.assignments.controller;

import edu.aiplatform.assignments.service.AssignmentAiDraftService;
import edu.aiplatform.assignments.dto.AssignmentViews;
import edu.aiplatform.identity.contract.Actor;
import java.util.UUID;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** AI soạn bản nháp (F6): gửi yêu cầu, xem đề xuất, áp dụng, bỏ. */
@RestController
@RequestMapping("/api/v1/assignments/{id}/ai-drafts")
public class AssignmentAiDraftController {

    private final AssignmentAiDraftService assignmentAiDraftService;

    public AssignmentAiDraftController(AssignmentAiDraftService assignmentAiDraftService) {
        this.assignmentAiDraftService = assignmentAiDraftService;
    }

    @PostMapping(consumes = "application/json")
    public AssignmentViews.AiDraft request(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                           @RequestBody AssignmentRequests.AiDraft body) {
        return assignmentAiDraftService.request(actor, id, body.buildCommand());
    }

    @GetMapping("/{draftId}")
    public AssignmentViews.AiDraft loadDraft(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                       @PathVariable UUID draftId) {
        return assignmentAiDraftService.loadDraft(actor, id, draftId);
    }

    /** Đề mẫu của môn để chọn khi soạn bằng AI; {@code own=true} thêm các đề cũ người soạn quản lý được. */
    @GetMapping("/templates")
    public List<AssignmentViews.AiTemplateOption> listTemplates(@AuthenticationPrincipal Actor actor,
                                                               @PathVariable UUID id,
                                                               @RequestParam(defaultValue = "false") boolean own) {
        return assignmentAiDraftService.listTemplates(actor, id, own);
    }

    @PostMapping(value = "/sources:preview", consumes = "application/json")
    public List<AssignmentViews.AiSourcePreview> previewSources(@AuthenticationPrincipal Actor actor,
                                                               @PathVariable UUID id,
                                                               @RequestBody AssignmentRequests.AiSources body) {
        return assignmentAiDraftService.previewSources(actor, id, body.sourceFileRefs());
    }

    @PostMapping(value = "/{draftId}:apply", consumes = "application/json")
    public AssignmentViews.Detail applyDraft(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                        @PathVariable UUID draftId, @RequestBody AssignmentRequests.Apply body) {
        return assignmentAiDraftService.applyDraft(actor, id, draftId, body.buildCommand());
    }

    @PostMapping("/{draftId}:discard")
    public AssignmentViews.AiDraft discard(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                           @PathVariable UUID draftId) {
        return assignmentAiDraftService.discard(actor, id, draftId);
    }
}
