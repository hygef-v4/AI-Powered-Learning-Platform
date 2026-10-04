package edu.aiplatform.academics.event;

import java.util.UUID;

public record EnrollmentActivated(UUID classId, UUID accountId) {}
