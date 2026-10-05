package edu.aiplatform.attempts.application;

import edu.aiplatform.attempts.port.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Khung cài đặt của U11: thân hàm tạm trả giá trị rỗng/an toàn hoặc báo chưa cài.
 */
@Service
public class AttemptQueryService implements SubmissionQueryPort {

    @Override
    public Optional<SubmissionView> get(UUID attemptId) {
        return Optional.empty();
    }

    @Override
    public List<SubmissionView> listByAssignment(UUID assignmentId) {
        return List.of();
    }

    @Override
    public Optional<SubmissionView> gradedAttempt(UUID assignmentId, UUID accountId) {
        return Optional.empty();
    }
}
