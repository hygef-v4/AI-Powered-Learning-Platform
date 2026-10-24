package edu.aiplatform.ai.contract;

/** Shared conservative estimate used by source pickers and shared API quota reservations. */
public final class AiTokenEstimate {
    private AiTokenEstimate() {
    }

    public static long estimate(int promptChars, int maxOutputTokens) {
        return (Math.max(0, promptChars) + 1L) / 2 + Math.max(0, maxOutputTokens);
    }
}
