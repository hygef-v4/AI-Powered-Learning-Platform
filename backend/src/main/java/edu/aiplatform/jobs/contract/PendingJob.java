package edu.aiplatform.jobs.contract;

import java.util.Map;

public record PendingJob(String idempotencyKey, Map<String, Object> payload) {
}
