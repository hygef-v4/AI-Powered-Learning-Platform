package edu.aiplatform.groupdocs.port;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record GroupSubmissionView(UUID groupDocumentId, UUID groupId, UUID assignmentId, String documentJson,
                                  Map<UUID, List<UUID>> sectionAuthors, Instant submittedAt, String submitMode, boolean late) {}
