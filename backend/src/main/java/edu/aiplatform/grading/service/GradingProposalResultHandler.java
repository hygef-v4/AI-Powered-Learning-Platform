package edu.aiplatform.grading.service;

import edu.aiplatform.ai.contract.AiTaskFailure;
import edu.aiplatform.ai.contract.AiTaskResult;
import edu.aiplatform.ai.contract.ApplyOutcome;
import edu.aiplatform.ai.contract.IAiTaskResultHandler;
import org.springframework.stereotype.Component;

@Component
public class GradingProposalResultHandler implements IAiTaskResultHandler {
    private final GradingProposalTransactions gradingProposalTransactions;

    /** Creates the handler that forwards AI task results to proposal transactions. */
    public GradingProposalResultHandler(GradingProposalTransactions gradingProposalTransactions) {
        this.gradingProposalTransactions = gradingProposalTransactions;
    }

    /** Returns the AI task type handled by this component. */
    public String taskType() {
        return "GRADING_PROPOSAL";
    }

    /** Applies a completed AI result to its grading proposal. */
    public ApplyOutcome onReady(AiTaskResult result) {
        return gradingProposalTransactions.acceptProposal(result);
    }

    /** Records a failed AI task against its grading proposal. */
    public void onFailed(AiTaskFailure failure) {
        gradingProposalTransactions.recordFailure(failure);
    }
}
