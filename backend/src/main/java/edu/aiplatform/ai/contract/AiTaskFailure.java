package edu.aiplatform.ai.contract;

import edu.aiplatform.identity.contract.Actor;
import java.util.UUID;

public record AiTaskFailure(UUID taskId, String taskType, Actor actor, String targetRef, AiTaskState state,
                            String safeError) {
}
