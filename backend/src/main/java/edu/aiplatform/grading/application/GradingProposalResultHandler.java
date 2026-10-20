package edu.aiplatform.grading.application;

import edu.aiplatform.ai.contract.AiTaskFailure;
import edu.aiplatform.ai.contract.AiTaskResult;
import edu.aiplatform.ai.contract.ApplyOutcome;
import edu.aiplatform.ai.contract.IAiTaskResultHandler;
import org.springframework.stereotype.Component;

@Component
public class GradingProposalResultHandler implements IAiTaskResultHandler {
    private final GradingProposalTransactions tx;

    public GradingProposalResultHandler(GradingProposalTransactions tx) {
        this.tx = tx;
    }

    public String taskType() {
        return "GRADING_PROPOSAL";
    }

    public ApplyOutcome onReady(AiTaskResult result) {
        return tx.accept(result);
    }

    public void onFailed(AiTaskFailure failure) {
        tx.failed(failure);
    }
}
