package edu.aiplatform.authoring.application;

import edu.aiplatform.assessments.port.*;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Khung cài đặt của U09: thân hàm tạm trả giá trị rỗng/an toàn hoặc báo chưa cài.
 */
@Service
public class TypeConfigService implements TypeConfigPort {

    @Override
    public List<ReviewIssue> check(UUID assignmentId) {
        return List.of();
    }

    @Override
    public void copy(UUID fromAssignmentId, UUID toAssignmentId) {
        // Chưa cài: U09.
    }

    @Override
    public void repointRubric(UUID oldRubricId, UUID newRubricId) {
        // Chưa cài: U09.
    }

    @Override
    public String readConfig(UUID assignmentId) {
        throw new UnsupportedOperationException("Chưa cài: U09");
    }
}
