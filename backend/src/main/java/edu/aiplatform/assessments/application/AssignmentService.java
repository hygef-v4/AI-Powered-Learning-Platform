package edu.aiplatform.assessments.application;

import edu.aiplatform.assessments.port.*;
import edu.aiplatform.questionbank.port.*;
import edu.aiplatform.shared.contract.ActorRef;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Khung cài đặt của U08: thân hàm tạm trả giá trị rỗng/an toàn hoặc báo chưa cài.
 * Plan U08: Bước 4.
 */
@Service
public class AssignmentService implements AssignmentDraftPort, RubricOwnerPort {

    @Override
    public AssignmentView createDraftFrom(UUID sourceAssignmentId, UUID targetClassId, UUID targetSubjectId) {
        throw new UnsupportedOperationException("Chưa cài: U08");
    }

    @Override
    public AssignmentView createTemplate(ActorRef actor, UUID subjectId, AssignmentType type, GradingMode mode, String title) {
        throw new UnsupportedOperationException("Chưa cài: U08");
    }

    @Override
    public void repoint(UUID oldRubricId, UUID newRubricId) {
        // Chưa cài: U08.
    }
}
