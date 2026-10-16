package edu.aiplatform.ai.contract;

import java.util.UUID;

public record AiTaskView(UUID taskId, AiTaskState state, AiSource source, AiProvider provider, String model,
                         ApplyOutcome applyResult, String safeError, AiUsageSummary usage) {
}
