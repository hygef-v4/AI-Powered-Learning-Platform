package edu.aiplatform.ai.contract;

/**
 * U06 khai báo, unit gọi cài (U07, U08, U09, U10): nhận kết quả task, kiểm quyền và phiên bản đích rồi áp dụng,
 * để U06 không phụ thuộc ngược unit gọi.
 */
public interface IAiTaskResultHandler {

    String taskType();

    ApplyOutcome onReady(AiTaskResult result);

    void onFailed(AiTaskFailure failure);
}
