package edu.aiplatform.grading.controller;

import edu.aiplatform.grading.service.GradingCommands.AiBatch;
import edu.aiplatform.grading.service.GradingCommands.AiRequest;
import edu.aiplatform.grading.service.GradingCommands.Batch;
import edu.aiplatform.grading.service.GradingProposalService;
import edu.aiplatform.grading.dto.GradingViews.BatchResult;
import edu.aiplatform.grading.dto.GradingViews.AiEstimate;
import edu.aiplatform.grading.model.GradingProposal;
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

    @PostMapping("/evaluations:ai-proposal-estimate")
    public AiEstimate estimateBatch(@AuthenticationPrincipal Actor actor, @Valid @RequestBody Batch input) {
        return service.estimateBatch(actor, input);
    }
}
