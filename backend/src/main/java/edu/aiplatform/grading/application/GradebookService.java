package edu.aiplatform.grading.application;

import edu.aiplatform.grading.port.*;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Khung cài đặt của U15: thân hàm tạm trả giá trị rỗng/an toàn hoặc báo chưa cài.
 */
@Service
public class GradebookService implements GradebookQueryPort {

    @Override
    public List<GradebookRow> forClass(UUID classId) {
        return List.of();
    }

    @Override
    public List<GradebookRow> forAssignment(UUID assignmentId) {
        return List.of();
    }
}
