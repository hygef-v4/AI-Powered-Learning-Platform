package edu.aiplatform.groupdocs.event;

import java.util.UUID;

public record GroupSubmitted(UUID groupDocumentId, UUID groupId, UUID assignmentId, String submitMode) {}
