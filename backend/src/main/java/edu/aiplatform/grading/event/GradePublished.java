package edu.aiplatform.grading.event;

import java.util.List;
import java.util.UUID;

public record GradePublished(UUID assignmentId, UUID classId, List<UUID> accountIds) {}
