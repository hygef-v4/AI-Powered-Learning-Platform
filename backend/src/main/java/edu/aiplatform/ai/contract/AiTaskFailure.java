package edu.aiplatform.ai.contract;

import edu.aiplatform.identity.contract.Actor;
import java.util.UUID;

public record AiTaskFailure(UUID taskId, String taskType, Actor actor, String targetRef, Long inputVersion,
                            AiTaskState state, String safeError) {

    public AiTaskFailure(UUID taskId, String taskType, Actor actor, String targetRef, AiTaskState state,
                         String safeError) {
        this(taskId, taskType, actor, targetRef, null, state, safeError);
    }
}
