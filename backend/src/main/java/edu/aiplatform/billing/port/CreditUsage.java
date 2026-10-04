package edu.aiplatform.billing.port;

import java.time.Instant;
import java.util.UUID;

public record CreditUsage(UUID suggestionId, String taskType, long creditsUsed, String creditStatus, Instant createdAt) {}
