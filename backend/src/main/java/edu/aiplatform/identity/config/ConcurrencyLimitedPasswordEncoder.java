package edu.aiplatform.identity.config;

import edu.aiplatform.shared.error.ApiException;
import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Cho tối đa {@code permits} lần bcrypt chạy cùng lúc; lần thừa xếp hàng theo thứ tự đến, chờ quá {@code queueWait}
 * thì trả 503 {@code SYSTEM_BUSY}.
 *
 * <p>Container bị giới hạn 2 CPU nhưng thấy mọi nhân của máy, nên nhiều luồng bcrypt cùng chạy làm hết quota CPU rồi bị
 * dừng chờ: 16 lần đăng nhập cùng lúc chỉ đạt 1,5 lượt/s, 2 lần cùng lúc đạt 3,4 lượt/s (TC-PERF-002).
 */
final class ConcurrencyLimitedPasswordEncoder implements PasswordEncoder {

    private final PasswordEncoder delegate;
    private final Semaphore permits;
    private final long queueWaitNanos;

    ConcurrencyLimitedPasswordEncoder(PasswordEncoder delegate, int permits, Duration queueWait) {
        if (permits < 1 || queueWait.isNegative()) {
            throw new IllegalArgumentException("Số bcrypt cùng lúc phải ≥ 1 và thời gian chờ không âm");
        }
        this.delegate = delegate;
        this.permits = new Semaphore(permits, true);
        this.queueWaitNanos = queueWait.toNanos();
    }

    @Override
    public String encode(CharSequence rawPassword) {
        acquirePermit();
        try {
            return delegate.encode(rawPassword);
        } finally {
            permits.release();
        }
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        acquirePermit();
        try {
            return delegate.matches(rawPassword, encodedPassword);
        } finally {
            permits.release();
        }
    }

    @Override
    public boolean upgradeEncoding(String encodedPassword) {
        return delegate.upgradeEncoding(encodedPassword);
    }

    // Chờ lượt bcrypt; hết thời gian chờ hoặc luồng bị ngắt thì báo bận thay vì để request treo tới timeout.
    private void acquirePermit() {
        try {
            if (permits.tryAcquire(queueWaitNanos, TimeUnit.NANOSECONDS)) {
                return;
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
        throw ApiException.createUnavailable("SYSTEM_BUSY", "Hệ thống tạm bận, vui lòng thử lại sau");
    }
}
