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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class EvaluationController {
    private final GradingService gradingService;
    private final SubmissionReviewService submissionReviewService;
    private final GradingBatchService gradingBatchService;
    private final GradingProposalTransactions gradingProposalTransactions;

    public EvaluationController(GradingService gradingService, SubmissionReviewService submissionReviewService, GradingBatchService gradingBatchService,
            GradingProposalTransactions gradingProposalTransactions) {
        this.gradingService = gradingService;
        this.submissionReviewService = submissionReviewService;
        this.gradingBatchService = gradingBatchService;
        this.gradingProposalTransactions = gradingProposalTransactions;
    }

    @GetMapping("/evaluations/{id}")
    public EvaluationDetail loadEvaluation(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
            @RequestParam(required = false) String status, @RequestParam(required = false) Boolean late) {
        return submissionReviewService.loadEvaluation(actor, id, status, late);
    }

    @PutMapping("/evaluations/{id}")
    public Evaluation saveEvaluation(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
            @Valid @RequestBody Save input) {
        return input.usedProposalId() == null ? gradingService.saveEvaluation(actor, id, input)
                : gradingProposalTransactions.saveUsingProposal(actor, id, input);
    }

    @PutMapping("/evaluations/{id}/members/{accountId}")
    public Evaluation saveMemberGrade(@AuthenticationPrincipal Actor actor, @PathVariable UUID id, @PathVariable UUID accountId,
            @Valid @RequestBody MemberSave input) {
        return gradingService.saveMemberGrade(actor, id, accountId, input);
    }

    @PostMapping("/evaluations:finalize")
    public BatchResult finalizeGrades(@AuthenticationPrincipal Actor actor, @Valid @RequestBody Batch input) {
        return gradingBatchService.executeBatch(actor, input, false);
    }

    @PostMapping("/evaluations:publish")
    public BatchResult publishGrades(@AuthenticationPrincipal Actor actor, @Valid @RequestBody Batch input) {
        return gradingBatchService.executeBatch(actor, input, true);
    }
}
