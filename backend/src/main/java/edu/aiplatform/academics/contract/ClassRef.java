package edu.aiplatform.academics.contract;

import java.util.UUID;

public record ClassRef(UUID classId, UUID subjectId, String code, String name, ClassStatus status, UUID teacherId,
                       boolean showGradeDistribution) {
}
