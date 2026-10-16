package edu.aiplatform.assignments.contract;

import java.util.UUID;

/**
 * U07 khai báo, U08 và U12 cài: phản ứng bắt buộc trong cùng transaction mở bài, ngưng giao.
 * Chưa có unit cài thì không làm gì.
 */
public interface IAssignmentLifecycleListener {

    void onOpened(UUID assignmentId);

    void onRetired(UUID assignmentId);
}
