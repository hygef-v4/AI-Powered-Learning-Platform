package edu.aiplatform.groupdocs.application;

import edu.aiplatform.groupdocs.port.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Khung cài đặt của U14: thân hàm tạm trả giá trị rỗng/an toàn hoặc báo chưa cài.
 */
@Service
public class GroupSubmissionQueryService implements GroupSubmissionQueryPort {

    @Override
    public Optional<GroupSubmissionView> latestSubmission(UUID groupDocumentId) {
        return Optional.empty();
    }

    @Override
    public List<GroupSubmissionView> listByAssignment(UUID assignmentId) {
        return List.of();
    }

    @Override
    public Map<UUID, Map<String, Integer>> sectionProgress(UUID assignmentId) {
        return Map.of();
    }
}
