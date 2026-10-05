package edu.aiplatform.groupdocs.port;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** U14 cung cấp cho U13, U15, U16. */
public interface GroupSubmissionQueryPort {
    Optional<GroupSubmissionView> latestSubmission(UUID groupDocumentId);
    List<GroupSubmissionView> listByAssignment(UUID assignmentId);
    /** Tiến độ phần: OPEN/CLAIMED/DONE theo nhóm, cho U15, U16. */
    Map<UUID, Map<String, Integer>> sectionProgress(UUID assignmentId);
}
