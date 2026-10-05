package edu.aiplatform.grading.port;

import java.util.List;
import java.util.UUID;

/** U15 cung cấp cho U16: điểm cuối đã chốt/công bố của bài GRADED, không có điểm AI. */
public interface GradebookQueryPort {
    List<GradebookRow> forClass(UUID classId);
    List<GradebookRow> forAssignment(UUID assignmentId);
}
