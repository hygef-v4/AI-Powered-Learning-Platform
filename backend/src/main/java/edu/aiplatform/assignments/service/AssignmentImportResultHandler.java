package edu.aiplatform.assignments.service;

import edu.aiplatform.ai.contract.AiTaskFailure;
import edu.aiplatform.ai.contract.AiTaskResult;
import edu.aiplatform.ai.contract.ApplyOutcome;
import edu.aiplatform.ai.contract.IAiTaskResultHandler;
import org.springframework.stereotype.Component;

/** Nhận kết quả tác vụ {@code ASSIGNMENT_IMPORT} của U06 (BR-U07-97, 99). */
@Component
public class AssignmentImportResultHandler implements IAiTaskResultHandler {

    private final AssignmentImportService assignmentImportService;

    public AssignmentImportResultHandler(AssignmentImportService assignmentImportService) {
        this.assignmentImportService = assignmentImportService;
    }

    @Override
    public String taskType() {
        return AssignmentImportService.TASK_TYPE;
    }

    @Override
    public ApplyOutcome onReady(AiTaskResult result) {
        return assignmentImportService.acceptResult(result);
    }

    @Override
    public void onFailed(AiTaskFailure failure) {
        assignmentImportService.rejectResult(failure);
    }
}
