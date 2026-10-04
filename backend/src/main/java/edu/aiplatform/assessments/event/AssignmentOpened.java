package edu.aiplatform.assessments.event;

import java.util.UUID;

public record AssignmentOpened(UUID assignmentId, UUID classId) {}
