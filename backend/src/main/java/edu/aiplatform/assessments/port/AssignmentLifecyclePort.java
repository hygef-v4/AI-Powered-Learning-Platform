package edu.aiplatform.assessments.port;

import java.util.UUID;

/** U08 khai báo, U11 và U14 cài; gọi trong transaction mở bài/ngưng giao. */
public interface AssignmentLifecyclePort {
    void onOpened(UUID assignmentId);
    void onRetired(UUID assignmentId);
}
