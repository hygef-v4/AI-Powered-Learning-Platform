package edu.aiplatform.assignmentwork.service;

import edu.aiplatform.ai.contract.AiTaskFailure;
import edu.aiplatform.ai.contract.AiTaskResult;
import edu.aiplatform.ai.contract.ApplyOutcome;
import edu.aiplatform.ai.contract.IAiTaskResultHandler;
import org.springframework.stereotype.Component;

/** Cài {@link IAiTaskResultHandler} cho {@code PRACTICE_FEEDBACK} (F9 bước 6). */
@Component
public class PracticeFeedbackResultHandler implements IAiTaskResultHandler {

    private final PracticeFeedbackService practiceFeedbackService;

    public PracticeFeedbackResultHandler(PracticeFeedbackService practiceFeedbackService) {
        this.practiceFeedbackService = practiceFeedbackService;
    }

    @Override
    public String taskType() {
        return PracticeFeedbackService.TASK_TYPE;
    }

    @Override
    public ApplyOutcome onReady(AiTaskResult result) {
        return practiceFeedbackService.handleReadyResult(result);
    }

    @Override
    public void onFailed(AiTaskFailure failure) {
        practiceFeedbackService.handleFailedResult(failure);
    }
}
