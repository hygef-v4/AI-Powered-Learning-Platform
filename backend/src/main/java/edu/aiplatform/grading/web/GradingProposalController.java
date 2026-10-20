package edu.aiplatform.grading.web;

import edu.aiplatform.grading.application.GradingCommands.AiBatch;
import edu.aiplatform.grading.application.GradingCommands.AiRequest;
import edu.aiplatform.grading.application.GradingProposalService;
import edu.aiplatform.grading.application.GradingViews.BatchResult;
import edu.aiplatform.grading.domain.GradingProposal;
import edu.aiplatform.identity.contract.Actor;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class GradingProposalController {
    private final GradingProposalService service;

    public GradingProposalController(GradingProposalService service) {
        this.service = service;
    }

    @PostMapping("/evaluations/{id}/ai-proposals")
    public GradingProposal request(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
            @Valid @RequestBody AiRequest input) {
        return service.request(actor, id, input);
    }

    @GetMapping("/evaluations/{id}/ai-proposals")
    public List<GradingProposal> list(@AuthenticationPrincipal Actor actor, @PathVariable UUID id) {
        return service.list(actor, id);
    }

    @PostMapping("/evaluations:ai-proposals")
    public BatchResult batch(@AuthenticationPrincipal Actor actor, @Valid @RequestBody AiBatch input) {
        return service.requestBatch(actor, input);
    }
}
