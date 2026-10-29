package edu.aiplatform.grading.contract;

import edu.aiplatform.assignments.contract.DocumentModel;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GroupSubmissionView(UUID groupDocumentId, UUID groupId, UUID assignmentId, UUID classId,
        DocumentModel content, List<UUID> memberIds, List<GroupSectionAuthor> sectionAuthors, Instant submittedAt,
        boolean late, String submissionHash) {
}
