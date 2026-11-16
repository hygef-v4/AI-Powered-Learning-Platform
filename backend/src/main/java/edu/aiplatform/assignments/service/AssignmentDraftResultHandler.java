package edu.aiplatform.assignments.service;

import edu.aiplatform.ai.contract.AiTaskFailure;
import edu.aiplatform.ai.contract.AiTaskResult;
import edu.aiplatform.ai.contract.ApplyOutcome;
import edu.aiplatform.ai.contract.IAiTaskResultHandler;
import org.springframework.stereotype.Component;

/** Cài {@link IAiTaskResultHandler} cho {@code ASSIGNMENT_DRAFT} (F6 bước 5); chạy trong worker của U06. */
@Component
public class AssignmentDraftResultHandler implements IAiTaskResultHandler {

    private final AssignmentAiDraftService assignmentAiDraftService;

    public AssignmentDraftResultHandler(AssignmentAiDraftService assignmentAiDraftService) {
        this.assignmentAiDraftService = assignmentAiDraftService;
    }

    @Override
    public String taskType() {
        return AssignmentAiDraftService.TASK_TYPE;
    }

    @Override
    public ApplyOutcome onReady(AiTaskResult result) {
        return assignmentAiDraftService.handleReadyResult(result);
    }

    @Override
    public void onFailed(AiTaskFailure failure) {
        assignmentAiDraftService.handleFailedResult(failure);
    }
}
