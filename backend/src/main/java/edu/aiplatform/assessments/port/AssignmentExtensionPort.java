package edu.aiplatform.assessments.port;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** U09, U10, U15, U16 ghi phần của mình khi trạng thái cho phép. */
public interface AssignmentExtensionPort {
    void writeConfig(UUID assignmentId, String configJson);
    /** Câu Text Essay: điểm bằng tổng rubric của câu (BR-U08-12, BR-U09-23). */
    void setQuestionPoints(UUID assignmentId, UUID questionId, BigDecimal points);
    void writeTemplateState(UUID assignmentId, AssignmentStatus status);
    void writeLineage(UUID assignmentId, UUID sourceAssignmentId, String origin);
    void markGradesReleased(UUID assignmentId, Instant releasedAt);
    void markReminderSent(UUID assignmentId, Instant sentAt);
}
