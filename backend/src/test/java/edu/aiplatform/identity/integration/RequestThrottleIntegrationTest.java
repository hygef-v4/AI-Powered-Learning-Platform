package edu.aiplatform.identity.integration;

import static org.assertj.core.api.Assertions.assertThat;

import edu.aiplatform.identity.IdentityTestData;
import edu.aiplatform.identity.config.IdentityProperties;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * {@link RequestThrottle} với Bucket4j Lettuce trên Redis 7 thật. Ngưỡng nhỏ và cooldown OTP 1 giây để chạy nhanh;
 * Bucket4j tính thời gian theo đồng hồ thật nên phần hết cooldown phải chờ thật.
 */
@Testcontainers(disabledWithoutDocker = true)
class RequestThrottleIntegrationTest {

    private static final int LOGIN_PER_IP = 3;
    private static final int PASSWORD_CHANGE_FAILURES = 2;
    private static final int OTP_PER_EMAIL = 3;
    private static final int OTP_PER_IP = 4;
    private static final Duration OTP_COOLDOWN = Duration.ofSeconds(1);

    @Container
    static final GenericContainer<?> REDIS = RedisTestSupport.container();

    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redis;

    private RequestThrottle throttle;

    @BeforeAll
    static void connect() {
        connectionFactory = RedisTestSupport.connectionFactory(REDIS);
        redis = new StringRedisTemplate(connectionFactory);
        redis.afterPropertiesSet();
    }

    @AfterAll
    static void disconnect() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @BeforeEach
    void setUp() {
        RedisTestSupport.flushAll(redis);
        throttle = new RequestThrottle(connectionFactory, properties());
    }

    @Test
    void loginIsBlockedPerIpAfterThresholdOfFailures() {
        for (int i = 0; i < LOGIN_PER_IP; i++) {
            assertThat(throttle.loginAllowed("10.0.0.1")).as("lần %d", i + 1).isTrue();
            throttle.recordLoginFailure("10.0.0.1");
        }
        assertThat(throttle.loginAllowed("10.0.0.1")).isFalse();
        assertThat(throttle.loginAllowed("10.0.0.2")).isTrue();

        // Bucket có hạn sống trên Redis, không nằm mãi.
        String key = RequestThrottle.PREFIX + "login-ip:10.0.0.1";
        assertThat(redis.hasKey(key)).isTrue();
        assertThat(redis.getExpire(key, TimeUnit.MILLISECONDS)).isPositive();
    }

    @Test
    void checkingLoginDoesNotSpendAttempts() {
        // Nhiều sinh viên đăng nhập đúng qua chung một IP (NAT của trường) không bị chặn.
        for (int i = 0; i < LOGIN_PER_IP * 5; i++) {
            assertThat(throttle.loginAllowed("10.0.0.7")).as("lần %d", i + 1).isTrue();
        }
    }

    @Test
    void passwordChangeIsBlockedPerAccountAfterFailures() {
        UUID account = UUID.randomUUID();
        for (int i = 0; i < PASSWORD_CHANGE_FAILURES; i++) {
            assertThat(throttle.passwordChangeAllowed(account)).isTrue();
            throttle.recordPasswordChangeFailure(account);
        }
        assertThat(throttle.passwordChangeAllowed(account)).isFalse();
        assertThat(throttle.passwordChangeAllowed(UUID.randomUUID())).isTrue();
    }

    @Test
    void otpRequestsForSameEmailRespectCooldown() throws InterruptedException {
        assertThat(throttle.tryOtpRequest("taivm@fpt.edu.vn", "10.0.0.1")).isTrue();
        assertThat(throttle.tryOtpRequest("taivm@fpt.edu.vn", "10.0.0.2")).isFalse();
        assertThat(throttle.tryOtpRequest("khoa@fpt.edu.vn", "10.0.0.3")).isTrue();

        Thread.sleep(OTP_COOLDOWN.plusMillis(200).toMillis());
        assertThat(throttle.tryOtpRequest("taivm@fpt.edu.vn", "10.0.0.4")).isTrue();
        assertThat(throttle.tryOtpRequest("taivm@fpt.edu.vn", "10.0.0.5")).isFalse();

        // Email chỉ nằm trong khóa ở dạng băm.
        Set<String> keys = redis.keys(RequestThrottle.PREFIX + "*");
        assertThat(keys).contains(RequestThrottle.PREFIX + "otp-email:" + Hashing.sha256Hex("taivm@fpt.edu.vn"));
        assertThat(keys).noneMatch(k -> k.contains("@"));
    }

    @Test
    void otpRequestsForSameEmailAreCappedPerHourEvenAfterCooldown() throws InterruptedException {
        for (int i = 0; i < OTP_PER_EMAIL; i++) {
            assertThat(throttle.tryOtpRequest("taivm@fpt.edu.vn", "10.0.1." + i)).as("lần %d", i + 1).isTrue();
            Thread.sleep(OTP_COOLDOWN.plusMillis(200).toMillis());
        }
        assertThat(throttle.tryOtpRequest("taivm@fpt.edu.vn", "10.0.1.99")).isFalse();
    }

    @Test
    void otpRequestsAreCappedPerIpBeforeTouchingEmailBucket() {
        for (int i = 0; i < OTP_PER_IP; i++) {
            assertThat(throttle.tryOtpRequest("sv" + i + "@fpt.edu.vn", "10.0.0.9")).as("lần %d", i + 1).isTrue();
        }
        assertThat(throttle.tryOtpRequest("moi@fpt.edu.vn", "10.0.0.9")).isFalse();

        // IP bị chặn thì chưa tiêu lượt của email: cùng email từ IP khác vẫn được ngay.
        assertThat(throttle.tryOtpRequest("moi@fpt.edu.vn", "10.0.0.10")).isTrue();
    }

    private static IdentityProperties properties() {
        IdentityProperties base = IdentityTestData.properties();
        return new IdentityProperties(base.allowedEmailDomains(), base.jwtSecret(), base.accessTtl(),
                base.refreshIdleTtl(), base.refreshMaxTtl(), base.bcryptCost(), base.otpTtl(), base.otpMaxAttempts(),
                base.loginLockThreshold(), base.loginLockDuration(), base.cookieSecure(), base.mailFrom(),
                base.bootstrapAdminEmail(), base.bootstrapAdminName(),
                new IdentityProperties.RateLimit(OTP_PER_EMAIL, OTP_COOLDOWN, OTP_PER_IP, LOGIN_PER_IP,
                        Duration.ofMinutes(5), PASSWORD_CHANGE_FAILURES, Duration.ofMinutes(15)));
    }
}
