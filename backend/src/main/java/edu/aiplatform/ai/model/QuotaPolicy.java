package edu.aiplatform.ai.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Hạn mức API chung (BR-U06-12): mức mặc định mỗi giảng viên mỗi ngày và trần toàn hệ thống; rỗng là chưa cấu hình,
 * khi đó chưa dùng được {@code SHARED}.
 */
public record QuotaPolicy(Long defaultDayTokens, Long globalDayTokens, long version, UUID updatedBy,
                          Instant updatedAt) {

    public static QuotaPolicy empty() {
        return new QuotaPolicy(null, null, 0, null, null);
    }

    public boolean configured() {
        return defaultDayTokens != null && globalDayTokens != null;
    }

    /** Mức riêng của giảng viên. */
    public record TeacherQuota(UUID accountId, long dayTokens, long version, UUID updatedBy, Instant updatedAt) {
    }

    /** Bộ đếm một cửa sổ ngày ({@code scopeKey} là accountId hoặc {@code GLOBAL}). */
    public record Window(String scopeKey, LocalDate day, long reservedTokens, long usedTokens) {

        public long consumed() {
            return reservedTokens + usedTokens;
        }
    }
}
