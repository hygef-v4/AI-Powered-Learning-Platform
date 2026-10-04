package edu.aiplatform.grading.application;

import edu.aiplatform.grading.port.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Khung cài đặt của U15: thân hàm tạm trả giá trị rỗng/an toàn hoặc báo chưa cài.
 */
@Service
public class GradingService implements GradeQueryPort {

    @Override
    public Optional<PublishedGrade> publishedForAttempt(UUID attemptId) {
        return Optional.empty();
    }

    @Override
    public List<PublishedGrade> publishedForStudentInClass(UUID accountId, UUID classId) {
        return List.of();
    }

    @Override
    public List<PublishedGrade> publishedForAssignment(UUID assignmentId) {
        return List.of();
    }
}
