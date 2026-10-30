package edu.aiplatform.assignments.service;

import edu.aiplatform.ai.contract.AiTaskFailure;
import edu.aiplatform.ai.contract.AiTaskResult;
import edu.aiplatform.ai.contract.ApplyOutcome;
import edu.aiplatform.ai.contract.IAiTaskResultHandler;
import org.springframework.stereotype.Component;

/** Cài {@link IAiTaskResultHandler} cho {@code ASSIGNMENT_DRAFT} (F6 bước 5); chạy trong worker của U06. */
@Component
public class AssignmentDraftResultHandler implements IAiTaskResultHandler {

    private final AssignmentAiDraftService drafts;

    public AssignmentDraftResultHandler(AssignmentAiDraftService drafts) {
        this.drafts = drafts;
    }

    @Override
    public String taskType() {
        return AssignmentAiDraftService.TASK_TYPE;
    }

    @Override
    public ApplyOutcome onReady(AiTaskResult result) {
        return drafts.onReady(result);
    }

    @Override
    public void onFailed(AiTaskFailure failure) {
        drafts.onFailed(failure);
    }
}
