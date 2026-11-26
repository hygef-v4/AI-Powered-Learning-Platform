package edu.aiplatform.assignmentwork.controller;

import edu.aiplatform.shared.error.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Cho tối đa {@code permits} request lưu nháp ({@code PUT /api/v1/attempts/{id}/content}) chạy cùng lúc; request thừa
 * xếp hàng theo thứ tự đến, chờ quá {@code queueWait} thì trả 503 {@code SYSTEM_BUSY}.
 *
 * <p>Một lần lưu 2.000 khối tốn vài trăm mili giây CPU: đọc body JSON ~430 KB, làm sạch, kiểm, ghi DB rồi ghi lại tài liệu
 * trong response. Khi 100 sinh viên cùng lưu, 100 luồng cùng tranh 2 CPU: luồng nhận phản hồi Redis bị đói CPU, kiểm
 * phiên quá 500 ms và trả 503 cho cả request nhẹ (TC-PERF-004, KAN-244). Lượt được lấy trước khi đọc body và trả sau khi
 * ghi xong response; luồng đang chờ lượt không tốn CPU nên các request khác vẫn chạy.
 */
@Component
public class SaveConcurrencyLimiter implements HandlerInterceptor {

    private static final String HOLDS_PERMIT = SaveConcurrencyLimiter.class.getName() + ".permit";

    private final Semaphore permits;
    private final long queueWaitNanos;

    public SaveConcurrencyLimiter(@Value("${platform.assignment-work.save-max-concurrent:0}") int maxConcurrent,
                                  @Value("${platform.assignment-work.save-queue-wait:30s}") Duration queueWait) {
        if (queueWait.isNegative()) {
            throw new IllegalArgumentException("Thời gian chờ lượt lưu không được âm");
        }
        int size = maxConcurrent > 0 ? maxConcurrent : Runtime.getRuntime().availableProcessors();
        this.permits = new Semaphore(size, true);
        this.queueWaitNanos = queueWait.toNanos();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!"PUT".equals(request.getMethod())) {
            return true;
        }
        acquirePermit();
        request.setAttribute(HOLDS_PERMIT, Boolean.TRUE);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (request.getAttribute(HOLDS_PERMIT) != null) {
            request.removeAttribute(HOLDS_PERMIT);
            permits.release();
        }
    }

    // Chờ lượt; hết thời gian chờ hoặc luồng bị ngắt thì báo bận (GlobalExceptionHandler ghi Problem Detail).
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
