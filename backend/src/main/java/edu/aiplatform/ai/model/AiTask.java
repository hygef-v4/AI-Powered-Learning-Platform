package edu.aiplatform.ai.model;

import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.ai.contract.AiSource;
import edu.aiplatform.ai.contract.AiTaskState;
import edu.aiplatform.ai.contract.ApplyOutcome;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Task AI của unit khác (domain-entities U06 mục 3). Prompt và kết quả chỉ giữ tới khi task kết thúc và đã chuyển
 * cho handler (BR-U06-24).
 */
public record AiTask(UUID id, String requestRef, String fingerprint, UUID actorId, AiSource source, UUID connectionId,
                     AiProvider provider, String model, String taskType, String targetRef, long inputVersion,
                     AiTaskState state, String inputPayload, String outputSchema, String resultPayload,
                     int maxOutputTokens, Long reservedTokens, LocalDate quotaDay, Instant dispatchedAt,
                     String providerRequestId, ApplyOutcome applyResult, boolean handlerDone, String safeError,
                     Instant createdAt, Instant finishedAt, long version) {

    /** Chuyển trạng thái hợp lệ (BR-U06-22). */
    private static final Map<AiTaskState, Set<AiTaskState>> NEXT = Map.of(
            AiTaskState.QUEUED, EnumSet.of(AiTaskState.RUNNING, AiTaskState.CANCELLED, AiTaskState.FAILED),
            AiTaskState.RUNNING, EnumSet.of(AiTaskState.READY, AiTaskState.FAILED, AiTaskState.UNKNOWN),
            AiTaskState.READY, EnumSet.noneOf(AiTaskState.class),
            AiTaskState.FAILED, EnumSet.noneOf(AiTaskState.class),
            AiTaskState.UNKNOWN, EnumSet.noneOf(AiTaskState.class),
            AiTaskState.CANCELLED, EnumSet.noneOf(AiTaskState.class));

    public static boolean canMove(AiTaskState from, AiTaskState to) {
        return NEXT.get(from).contains(to);
    }

    public boolean finished() {
        return NEXT.get(state).isEmpty();
    }

    public boolean active() {
        return state == AiTaskState.QUEUED || state == AiTaskState.RUNNING;
    }

    public AiTask moveTo(AiTaskState next, Instant now) {
        if (!canMove(state, next)) {
            throw new IllegalStateException("Task " + state + " không chuyển được sang " + next);
        }
        boolean done = NEXT.get(next).isEmpty();
        return new AiTask(id, requestRef, fingerprint, actorId, source, connectionId, provider, model, taskType, targetRef,
                inputVersion, next, inputPayload, outputSchema, resultPayload, maxOutputTokens, reservedTokens, quotaDay,
                next == AiTaskState.RUNNING ? now : dispatchedAt, providerRequestId, applyResult, handlerDone, safeError,
                createdAt, done ? now : finishedAt, version);
    }

    public AiTask withResult(String result, String requestId) {
        return new AiTask(id, requestRef, fingerprint, actorId, source, connectionId, provider, model, taskType, targetRef,
                inputVersion, state, inputPayload, outputSchema, result, maxOutputTokens, reservedTokens, quotaDay,
                dispatchedAt, requestId, applyResult, handlerDone, safeError, createdAt, finishedAt, version);
    }

    public AiTask withError(String error) {
        String safe = error == null ? null : error.length() > 500 ? error.substring(0, 500) : error;
        return new AiTask(id, requestRef, fingerprint, actorId, source, connectionId, provider, model, taskType, targetRef,
                inputVersion, state, inputPayload, outputSchema, resultPayload, maxOutputTokens, reservedTokens, quotaDay,
                dispatchedAt, providerRequestId, applyResult, handlerDone, safe, createdAt, finishedAt, version);
    }

    /** Đã chuyển cho handler: xóa prompt và kết quả, chỉ giữ thông tin task (P6). */
    public AiTask handled(ApplyOutcome outcome) {
        return new AiTask(id, requestRef, fingerprint, actorId, source, connectionId, provider, model, taskType, targetRef,
                inputVersion, state, null, null, null, maxOutputTokens, reservedTokens, quotaDay, dispatchedAt,
                providerRequestId, outcome, true, safeError, createdAt, finishedAt, version);
    }
}
