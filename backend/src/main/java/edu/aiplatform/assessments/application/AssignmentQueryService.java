package edu.aiplatform.assessments.application;

import edu.aiplatform.assessments.port.*;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Khung cài đặt của U08: thân hàm tạm trả giá trị rỗng/an toàn hoặc báo chưa cài.
 * Plan U08: Bước 9.
 */
@Service
public class AssignmentQueryService implements AssignmentQueryPort {

    @Override
    public Optional<AssignmentView> get(UUID assignmentId) {
        return Optional.empty();
    }

    @Override
    public List<AssignmentView> listForClass(UUID classId) {
        return List.of();
    }

    @Override
    public SubmissionWindow isSubmissionOpen(UUID assignmentId, Instant now) {
        throw new UnsupportedOperationException("Chưa cài: U08");
    }
}
