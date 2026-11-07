package edu.aiplatform.grading.service;

import edu.aiplatform.ai.contract.AiTaskFailure;
import edu.aiplatform.ai.contract.AiTaskResult;
import edu.aiplatform.ai.contract.ApplyOutcome;
import edu.aiplatform.ai.contract.IAiTaskResultHandler;
import org.springframework.stereotype.Component;

@Component
public class GradingProposalResultHandler implements IAiTaskResultHandler {
    private final GradingProposalTransactions gradingProposalTransactions;

    public GradingProposalResultHandler(GradingProposalTransactions gradingProposalTransactions) {
        this.gradingProposalTransactions = gradingProposalTransactions;
    }

    public String taskType() {
        return "GRADING_PROPOSAL";
    }

    public ApplyOutcome onReady(AiTaskResult result) {
        return gradingProposalTransactions.acceptProposal(result);
    }

    public void onFailed(AiTaskFailure failure) {
        gradingProposalTransactions.recordFailure(failure);
    }
}
