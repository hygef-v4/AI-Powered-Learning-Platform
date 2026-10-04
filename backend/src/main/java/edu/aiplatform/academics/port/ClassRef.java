package edu.aiplatform.academics.port;

import java.util.UUID;

public record ClassRef(UUID classId, UUID subjectId, ClassStatus status, UUID teacherId, boolean showGradeDistribution) {}
