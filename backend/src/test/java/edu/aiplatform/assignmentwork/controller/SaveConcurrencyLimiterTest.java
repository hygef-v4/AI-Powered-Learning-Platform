package edu.aiplatform.assignmentwork.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.aiplatform.shared.error.ApiException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.springframework.web.util.ServletRequestPathUtils;

/** TC-PERF-004 (KAN-244): request lưu nháp chạy tối đa N cái cùng lúc, request thừa chờ rồi báo bận. */
class SaveConcurrencyLimiterTest {

    private static MockHttpServletRequest put() {
        return new MockHttpServletRequest("PUT", "/api/v1/attempts/a/content");
    }

    // DispatcherServlet tách path trước khi so khớp interceptor; test làm thay bước đó.
    private static MockHttpServletRequest parsed(MockHttpServletRequest request) {
        ServletRequestPathUtils.parseAndCache(request);
        return request;
    }

    private static void run(SaveConcurrencyLimiter limiter, MockHttpServletRequest request, Runnable work) {
        var response = new MockHttpServletResponse();
        assertThat(limiter.preHandle(request, response, new Object())).isTrue();
        try {
            work.run();
        } finally {
            limiter.afterCompletion(request, response, new Object(), null);
        }
    }

    @Test
    void neverRunsMoreThanThePermitCountAtOnce() throws Exception {
        var limiter = new SaveConcurrencyLimiter(2, Duration.ofSeconds(10));
        var running = new AtomicInteger();
        var peak = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Future<?>> calls = new ArrayList<>();
            for (int i = 0; i < 16; i++) {
                calls.add(pool.submit(() -> run(limiter, put(), () -> {
                    peak.accumulateAndGet(running.incrementAndGet(), Math::max);
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    running.decrementAndGet();
                })));
            }
            for (var call : calls) {
                call.get(10, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }
        assertThat(peak.get()).isEqualTo(2);
    }

    @Test
    void reportsBusyWhenTheQueueWaitRunsOutAndKeepsThePermitOfTheRunningSave() {
        var limiter = new SaveConcurrencyLimiter(1, Duration.ofMillis(50));
        var holder = put();
        var response = new MockHttpServletResponse();
        assertThat(limiter.preHandle(holder, response, new Object())).isTrue();

        var waiting = put();
        assertThatThrownBy(() -> limiter.preHandle(waiting, new MockHttpServletResponse(), new Object()))
                .isInstanceOfSatisfying(ApiException.class, error -> {
                    assertThat(error.readCode()).isEqualTo("SYSTEM_BUSY");
                    assertThat(error.readStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                });
        // Request bị từ chối không giữ lượt nên afterCompletion của nó không trả thêm lượt.
        limiter.afterCompletion(waiting, new MockHttpServletResponse(), new Object(), null);
        assertThatThrownBy(() -> limiter.preHandle(put(), new MockHttpServletResponse(), new Object()))
                .isInstanceOf(ApiException.class);

        limiter.afterCompletion(holder, response, new Object(), new IllegalStateException("lỗi lưu"));
        run(limiter, put(), () -> { });
    }

    @Test
    void releasesThePermitOnlyOncePerRequest() {
        var limiter = new SaveConcurrencyLimiter(1, Duration.ofMillis(50));
        var request = put();
        limiter.preHandle(request, new MockHttpServletResponse(), new Object());
        limiter.afterCompletion(request, new MockHttpServletResponse(), new Object(), null);
        limiter.afterCompletion(request, new MockHttpServletResponse(), new Object(), null);
        var first = put();
        limiter.preHandle(first, new MockHttpServletResponse(), new Object());
        assertThatThrownBy(() -> limiter.preHandle(put(), new MockHttpServletResponse(), new Object()))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void ignoresOtherMethodsOnTheSamePath() {
        var limiter = new SaveConcurrencyLimiter(1, Duration.ofMillis(50));
        limiter.preHandle(put(), new MockHttpServletResponse(), new Object());
        var get = new MockHttpServletRequest("GET", "/api/v1/attempts/a/content");
        assertThat(limiter.preHandle(get, new MockHttpServletResponse(), new Object())).isTrue();
    }

    @Test
    void isRegisteredOnlyForTheAttemptContentPath() {
        var limiter = new SaveConcurrencyLimiter(1, Duration.ofMillis(50));
        var registry = new InterceptorRegistry() {
            List<Object> registered() {
                return getInterceptors();
            }
        };
        new WorkWebConfig(limiter).addInterceptors(registry);
        assertThat(registry.registered()).singleElement().isInstanceOfSatisfying(MappedInterceptor.class, mapped -> {
            assertThat(mapped.getInterceptor()).isSameAs(limiter);
            assertThat(mapped.matches(parsed(put()))).isTrue();
            assertThat(mapped.matches(parsed(new MockHttpServletRequest("PUT", "/api/v1/attempts/a/submit")))).isFalse();
            assertThat(mapped.matches(parsed(new MockHttpServletRequest("PUT", "/api/v1/group-docs/a/sections/b/content"))))
                    .isFalse();
        });
    }

    @Test
    void rejectsANegativeQueueWait() {
        assertThatThrownBy(() -> new SaveConcurrencyLimiter(1, Duration.ofMillis(-1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
