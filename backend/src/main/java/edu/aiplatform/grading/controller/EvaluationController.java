package edu.aiplatform.grading.controller;

import edu.aiplatform.grading.service.GradingBatchService;
import edu.aiplatform.grading.service.GradingCommands.Batch;
import edu.aiplatform.grading.service.GradingCommands.MemberSave;
import edu.aiplatform.grading.service.GradingCommands.Save;
import edu.aiplatform.grading.service.GradingProposalTransactions;
import edu.aiplatform.grading.service.GradingService;
import edu.aiplatform.grading.dto.GradingViews.BatchResult;
import edu.aiplatform.grading.dto.GradingViews.EvaluationDetail;
import edu.aiplatform.grading.service.SubmissionReviewService;
import edu.aiplatform.grading.model.Evaluation;
import edu.aiplatform.identity.contract.Actor;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class EvaluationController {
    private final GradingService grading;
    private final SubmissionReviewService review;
    private final GradingBatchService batch;
    private final GradingProposalTransactions proposals;

    public EvaluationController(GradingService grading, SubmissionReviewService review, GradingBatchService batch,
            GradingProposalTransactions proposals) {
        this.grading = grading;
        this.review = review;
        this.batch = batch;
        this.proposals = proposals;
    }

    @GetMapping("/evaluations/{id}")
    public EvaluationDetail detail(@AuthenticationPrincipal Actor actor, @PathVariable UUID id) {
        return review.detail(actor, id);
    }

    @PutMapping("/evaluations/{id}")
    public Evaluation save(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
            @Valid @RequestBody Save input) {
        return input.usedProposalId() == null ? grading.save(actor, id, input)
                : proposals.saveUsingProposal(actor, id, input);
    }

    @PutMapping("/evaluations/{id}/members/{accountId}")
    public Evaluation member(@AuthenticationPrincipal Actor actor, @PathVariable UUID id, @PathVariable UUID accountId,
            @Valid @RequestBody MemberSave input) {
        return grading.saveMember(actor, id, accountId, input);
    }

    @PostMapping("/evaluations:finalize")
    public BatchResult finalizeGrades(@AuthenticationPrincipal Actor actor, @Valid @RequestBody Batch input) {
        return batch.run(actor, input, false);
    }

    @PostMapping("/evaluations:publish")
    public BatchResult publish(@AuthenticationPrincipal Actor actor, @Valid @RequestBody Batch input) {
        return batch.run(actor, input, true);
    }
}
