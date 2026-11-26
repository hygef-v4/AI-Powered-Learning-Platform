package edu.aiplatform.identity.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.aiplatform.shared.error.ApiException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

/** TC-PERF-002: bcrypt chạy tối đa N lần cùng lúc, lần thừa chờ rồi báo bận. */
class ConcurrencyLimitedPasswordEncoderTest {

    @Test
    void delegatesHashingAndMatching() {
        var encoder = new ConcurrencyLimitedPasswordEncoder(new PlainEncoder(null, null), 1, Duration.ofSeconds(1));
        assertThat(encoder.encode("secret")).isEqualTo("plain:secret");
        assertThat(encoder.matches("secret", "plain:secret")).isTrue();
        assertThat(encoder.matches("other", "plain:secret")).isFalse();
        assertThat(encoder.upgradeEncoding("plain:secret")).isFalse();
    }

    @Test
    void neverRunsMoreThanThePermitCountAtOnce() throws Exception {
        var running = new AtomicInteger();
        var peak = new AtomicInteger();
        var encoder = new ConcurrencyLimitedPasswordEncoder(new PlainEncoder(running, peak), 2, Duration.ofSeconds(10));
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Future<Boolean>> calls = new ArrayList<>();
            for (int i = 0; i < 16; i++) {
                calls.add(pool.submit(() -> encoder.matches("secret", "plain:secret")));
            }
            for (var call : calls) {
                assertThat(call.get(10, TimeUnit.SECONDS)).isTrue();
            }
        } finally {
            pool.shutdownNow();
        }
        assertThat(peak.get()).isEqualTo(2);
    }

    @Test
    void reportsBusyWhenTheQueueWaitRunsOut() throws Exception {
        var release = new CountDownLatch(1);
        var started = new CountDownLatch(1);
        PasswordEncoder blocking = new PlainEncoder(null, null) {
            @Override
            public boolean matches(CharSequence raw, String encoded) {
                started.countDown();
                try {
                    release.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return true;
            }
        };
        var encoder = new ConcurrencyLimitedPasswordEncoder(blocking, 1, Duration.ofMillis(50));
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<Boolean> holder = pool.submit(() -> encoder.matches("a", "plain:a"));
            assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> encoder.matches("b", "plain:b"))
                    .isInstanceOfSatisfying(ApiException.class, error -> {
                        assertThat(error.readCode()).isEqualTo("SYSTEM_BUSY");
                        assertThat(error.readStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    });
            release.countDown();
            assertThat(holder.get(5, TimeUnit.SECONDS)).isTrue();
            assertThat(encoder.matches("c", "plain:c")).isTrue();
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void rejectsInvalidLimits() {
        assertThatThrownBy(() -> new ConcurrencyLimitedPasswordEncoder(new PlainEncoder(null, null), 0,
                Duration.ofSeconds(1))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ConcurrencyLimitedPasswordEncoder(new PlainEncoder(null, null), 1,
                Duration.ofSeconds(-1))).isInstanceOf(IllegalArgumentException.class);
    }

    /** Bộ băm giả, đếm số lần đang chạy cùng lúc khi được cấp bộ đếm. */
    private static class PlainEncoder implements PasswordEncoder {
        private final AtomicInteger running;
        private final AtomicInteger peak;

        PlainEncoder(AtomicInteger running, AtomicInteger peak) {
            this.running = running;
            this.peak = peak;
        }

        @Override
        public String encode(CharSequence raw) {
            return "plain:" + raw;
        }

        @Override
        public boolean matches(CharSequence raw, String encoded) {
            if (running != null) {
                peak.accumulateAndGet(running.incrementAndGet(), Math::max);
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    running.decrementAndGet();
                }
            }
            return encode(raw).equals(encoded);
        }
    }
}
