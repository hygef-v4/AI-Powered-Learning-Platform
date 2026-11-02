package edu.aiplatform.quizzes.contract;

import java.math.BigDecimal;

/**
 * Mức trừ gợi ý theo số lần rời trang (BR-U10-15), giảng viên quyết định có trừ hay không: miễn {@code freeLeaves} lần đầu, mỗi lần sau trừ {@code pointsPerLeave},
 * tổng không quá {@code maxPenalty} (trống là không giới hạn). Chỉ đếm khoảng rời chắc chắn dài ít nhất
 * {@code minAwaySeconds} giây.
 */
public record LeavePenalty(int freeLeaves, BigDecimal pointsPerLeave, BigDecimal maxPenalty, int minAwaySeconds) {
}
