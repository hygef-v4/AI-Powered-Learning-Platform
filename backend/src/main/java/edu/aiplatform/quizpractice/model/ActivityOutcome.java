package edu.aiplatform.quizpractice.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Số liệu rời trang và mức trừ gợi ý chụp vào lượt khi nộp (BR-U11-32, 33), vì sự kiện gốc chỉ giữ 1 ngày. {@code rawScore}
 * là điểm chấm từ câu trả lời; {@code suggestedPenalty} chỉ là gợi ý; {@code penaltyPoints} là điểm giảng viên đã quyết
 * định trừ, kèm người quyết định, lúc quyết định và lý do.
 */
public record ActivityOutcome(BigDecimal rawScore, BigDecimal suggestedPenalty, BigDecimal penaltyPoints,
                              Integer leaveCount, ActivitySignal signal, PenaltyDecision decision, UUID decidedBy,
                              Instant decidedAt, String decisionReason) {

    /** Chỉ lượt có mức trừ gợi ý mới cần giảng viên quyết định. */
    public boolean decidable() {
        return suggestedPenalty != null && suggestedPenalty.signum() > 0;
    }

    public boolean pendingDecision() {
        return decidable() && decision == null;
    }
}
