package edu.aiplatform.grading.model;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

public class GradingProposal {
    public UUID id;
    public UUID evaluationId;
    public UUID requesterId;
    public UUID taskId;
    public String taskState;
    public long versionNo;
    public String requestRef;
    public String submissionHash;
    public String rubricFingerprint;
    public String status = "PENDING";
    public String origin = "AI_TASK";
    public String source;
    public String provider;
    public String model;
    public String agentLabel;
    public String safeError;
    public JsonNode proposal;
    public JsonNode usage;
    public Instant createdAt;
    public Instant completedAt;
    public Instant usedAt;
}
