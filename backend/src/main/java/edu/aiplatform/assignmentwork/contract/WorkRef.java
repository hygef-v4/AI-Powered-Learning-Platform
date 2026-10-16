package edu.aiplatform.assignmentwork.contract;

import java.util.UUID;

public record WorkRef(WorkKind kind, UUID workId, UUID classId) {
}
