package edu.aiplatform.ai.service;

import edu.aiplatform.ai.model.QuotaPolicy;
import edu.aiplatform.ai.model.QuotaPolicy.Window;
import edu.aiplatform.ai.repository.QuotaRepository;
import edu.aiplatform.ai.contract.AiTokenEstimate;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Giữ chỗ và quyết toán quota API chung theo ngày Asia/Ho_Chi_Minh (BR-U06-12–15, P1). Khóa cửa sổ toàn hệ thống
 * trước, của người gọi sau (NFR-U06-01); chạy trong transaction của bên gọi.
 */
@Service
public class QuotaReservationService {

    public static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private final QuotaRepository repository;
    private final Clock clock;

    public QuotaReservationService(QuotaRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public LocalDate resolveToday() {
        return LocalDate.now(clock.withZone(VN));
    }

    /** Ước tính: số ký tự prompt chia 2 làm tròn lên, cộng {@code maxOutputTokens} (BR-U06-13). */
    public static long estimate(int promptChars, int maxOutputTokens) {
        return AiTokenEstimate.estimate(promptChars, maxOutputTokens);
    }

    /** Mức ngày của một giảng viên: mức riêng nếu có (0 là chặn), không thì mức mặc định; chưa cấu hình là rỗng. */
    @Transactional(readOnly = true)
    public Long limitOf(UUID accountId) {
        QuotaPolicy policy = repository.loadPolicy();
        if (!policy.hasLimits()) {
            return null;
        }
        return repository.findTeacherQuota(accountId).map(QuotaPolicy.TeacherQuota::dayTokens)
                .orElse(policy.defaultDayTokens());
    }

    /** Còn lại hôm nay của người gọi, đã tính trần toàn hệ thống; rỗng khi chưa cấu hình. */
    @Transactional(readOnly = true)
    public Remaining calculateRemaining(UUID accountId) {
        QuotaPolicy policy = repository.loadPolicy();
        Long limit = limitOf(accountId);
        if (limit == null) {
            return null;
        }
        LocalDate day = resolveToday();
        long own = repository.findWindow(accountId.toString(), day).map(Window::sumConsumed).orElse(0L);
        long global = repository.findWindow(QuotaRepository.GLOBAL, day).map(Window::sumConsumed).orElse(0L);
        long left = Math.max(0, Math.min(limit - own, policy.globalDayTokens() - global));
        return new Remaining(own, limit, left, calculateNextReset());
    }

    @Transactional(propagation = Propagation.MANDATORY, noRollbackFor = QuotaUnavailableException.class)
    public Reservation reserve(UUID accountId, long tokens) {
        QuotaPolicy policy = repository.loadPolicy();
        if (!policy.hasLimits()) {
            throw createExceededError("AI_QUOTA_NOT_CONFIGURED", "API của trường chưa được cấu hình hạn mức");
        }
        long limit = repository.findTeacherQuota(accountId).map(QuotaPolicy.TeacherQuota::dayTokens)
                .orElse(policy.defaultDayTokens());
        if (limit == 0) {
            throw createExceededError("AI_QUOTA_BLOCKED", "Bạn không được dùng API của trường; hãy chọn API cá nhân");
        }
        LocalDate day = resolveToday();
        String own = accountId.toString();
        repository.ensureWindow(QuotaRepository.GLOBAL, day);
        repository.ensureWindow(own, day);
        Window global = repository.lockWindow(QuotaRepository.GLOBAL, day);
        Window mine = repository.lockWindow(own, day);
        if (global.sumConsumed() + tokens > policy.globalDayTokens()) {
            throw createExceededError("AI_QUOTA_GLOBAL_EXCEEDED",
                    "API của trường đã hết hạn mức hôm nay; đặt lại lúc 00:00 hoặc chọn API cá nhân");
        }
        if (mine.sumConsumed() + tokens > limit) {
            throw createExceededError("AI_QUOTA_EXCEEDED", "Bạn đã dùng hết hạn mức hôm nay; đặt lại lúc 00:00 hoặc chọn API cá nhân");
        }
        repository.adjust(QuotaRepository.GLOBAL, day, tokens, 0);
        repository.adjust(own, day, tokens, 0);
        return new Reservation(day, tokens);
    }

    /** Provider trả usage: thay phần giữ chỗ bằng token thật, kể cả vượt ước tính (BR-U06-14, 15). */
    @Transactional(propagation = Propagation.MANDATORY)
    public void settle(UUID accountId, LocalDate day, long reserved, long actual) {
        repository.adjust(QuotaRepository.GLOBAL, day, -reserved, actual);
        repository.adjust(accountId.toString(), day, -reserved, actual);
    }

    /** Provider từ chối hoặc hủy trước khi gửi: giải phóng giữ chỗ (BR-U06-14, 26). */
    @Transactional(propagation = Propagation.MANDATORY)
    public void release(UUID accountId, LocalDate day, long reserved) {
        settle(accountId, day, reserved, 0);
    }

    /** Không biết kết quả: tính phần giữ chỗ là đã dùng, không tự hoàn (BR-U06-14). */
    @Transactional(propagation = Propagation.MANDATORY)
    public void chargeReserved(UUID accountId, LocalDate day, long reserved) {
        settle(accountId, day, reserved, reserved);
    }

    public ZonedDateTime calculateNextReset() {
        return resolveToday().plusDays(1).atStartOfDay(VN);
    }

    private ApiException createExceededError(String code, String message) {
        return new QuotaUnavailableException(code, message + " (cửa sổ mới lúc " + calculateNextReset() + ")");
    }

    public record Reservation(LocalDate day, long tokens) {
    }

    public record Remaining(long used, long limit, long left, ZonedDateTime resetAt) {
    }
}
