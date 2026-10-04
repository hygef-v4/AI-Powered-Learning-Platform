package edu.aiplatform.assessments.application;

import edu.aiplatform.assessments.port.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Khung cài đặt của U08: thân hàm tạm trả giá trị rỗng/an toàn hoặc báo chưa cài.
 * Plan U08: Bước 9.
 */
@Service
public class AssignmentExtensionService implements AssignmentExtensionPort {

    @Override
    public void writeConfig(UUID assignmentId, String configJson) {
        // Chưa cài: U08.
    }

    @Override
    public void setQuestionPoints(UUID assignmentId, UUID questionId, BigDecimal points) {
        // Chưa cài: U08.
    }

    @Override
    public void writeTemplateState(UUID assignmentId, AssignmentStatus status) {
        // Chưa cài: U08.
    }

    @Override
    public void writeLineage(UUID assignmentId, UUID sourceAssignmentId, String origin) {
        // Chưa cài: U08.
    }

    @Override
    public void markGradesReleased(UUID assignmentId, Instant releasedAt) {
        // Chưa cài: U08.
    }

    @Override
    public void markReminderSent(UUID assignmentId, Instant sentAt) {
        // Chưa cài: U08.
    }
}
