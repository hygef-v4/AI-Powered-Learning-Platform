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
    private final GradingProposalService gradingProposalService;

    /** Creates the controller with the grading proposal service. */
    public GradingProposalController(GradingProposalService gradingProposalService) {
        this.gradingProposalService = gradingProposalService;
    }

    /**
     * Generates an asynchronous AI rubric grading proposal for a single submission.
     */
    @PostMapping("/evaluations/{id}/ai-proposals")
    public GradingProposal requestProposal(@AuthenticationPrincipal Actor actor,
            @PathVariable UUID id, @Valid @RequestBody AiRequest input) {
        return gradingProposalService.requestProposal(actor, id, input);
    }

    /**
     * Lists all historical AI grading proposals generated for a submission.
     */
    @GetMapping("/evaluations/{id}/ai-proposals")
    public List<GradingProposal> listProposals(@AuthenticationPrincipal Actor actor,
            @PathVariable UUID id) {
        return gradingProposalService.listProposals(actor, id);
    }

    /** Requests AI grading proposals for a batch of evaluations. */
    @PostMapping("/evaluations:ai-proposals")
    public BatchResult requestProposals(@AuthenticationPrincipal Actor actor,
            @Valid @RequestBody AiBatch input) {
        return gradingProposalService.requestProposals(actor, input);
    }

    /** Estimates token usage for a batch without requesting AI proposals. */
    @PostMapping("/evaluations:ai-proposal-estimate")
    public AiEstimate estimateProposalTokens(@AuthenticationPrincipal Actor actor,
            @Valid @RequestBody Batch input) {
        return gradingProposalService.estimateProposalTokens(actor, input);
    }
}
