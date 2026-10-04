package edu.aiplatform.assessments.port;

import edu.aiplatform.questionbank.port.AssignmentType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AssignmentView(UUID id, UUID classId, UUID subjectId, AssignmentType type, GradingMode gradingMode,
                             String title, String instructions, String configJson, AssignmentStatus status, int version,
                             Instant opensAt, Instant closesAt, Instant lateUntil, int maxAttempts,
                             List<AssignmentQuestionRef> questions) {}
