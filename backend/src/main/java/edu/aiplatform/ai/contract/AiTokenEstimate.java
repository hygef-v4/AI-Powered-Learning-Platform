package edu.aiplatform.ai.contract;

/** Shared conservative estimate used by source pickers and shared API quota reservations. */
public final class AiTokenEstimate {
    private AiTokenEstimate() {
    }

    /** Mỗi ảnh trong prompt tính như 3.200 ký tự, tức khoảng 1.600 token theo công thức dưới (BR-U06-56). */
    public static final int IMAGE_PROMPT_CHARS = 3_200;

    public static long estimate(int promptChars, int maxOutputTokens) {
        return (Math.max(0, promptChars) + 1L) / 2 + Math.max(0, maxOutputTokens);
    }
}
