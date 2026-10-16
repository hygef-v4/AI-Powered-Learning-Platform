package edu.aiplatform.assignmentwork.contract;

import java.time.Instant;
import java.util.UUID;

public record ActivityEventInput(UUID sessionId, String clientEventId, long sequence, ActivityKind kind,
                                 Instant clientTime) {
}
