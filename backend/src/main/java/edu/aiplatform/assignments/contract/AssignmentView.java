package edu.aiplatform.assignments.contract;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AssignmentView(UUID assignmentId, AssignmentScope scope, UUID classId, UUID subjectId, int versionNo,
                             String title, SubmissionMode submissionMode, GradingMode gradingMode,
                             boolean diagramEnabled, AssignmentStatus status, Instant opensAt, Instant closesAt,
                             Instant lateUntil, Integer maxAttempts, boolean activityMonitoringEnabled,
                             String activityNotice, List<UUID> referenceMaterialIds) {
}
