package edu.aiplatform.ai.contract;

import edu.aiplatform.identity.contract.Actor;
import java.util.UUID;

/** {@code output} là JSON đã khớp {@code outputSchema}. */
public record AiTaskResult(UUID taskId, String taskType, Actor actor, String targetRef, long inputVersion,
                           String output) {
}
