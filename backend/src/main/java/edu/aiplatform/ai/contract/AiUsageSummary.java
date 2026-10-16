package edu.aiplatform.ai.contract;

public record AiUsageSummary(Long inputTokens, Long outputTokens, UsageMeasurement measurement) {
}
