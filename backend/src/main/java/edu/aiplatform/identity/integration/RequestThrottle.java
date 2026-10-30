package edu.aiplatform.identity.integration;

import edu.aiplatform.identity.config.IdentityProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConfigurationBuilder;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.lettuce.core.RedisClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.stereotype.Component;

/**
 * Giới hạn tần suất endpoint public bằng Bucket4j trên Redis (P6, NFR-U01-20, 21). Email trong khóa được băm để Redis
 * không chứa email rõ. Kết nối Bucket4j tạo lúc dùng lần đầu: Redis lỗi lúc khởi động không chặn backend, còn lúc
 * gọi thì ném lỗi để service trả "hệ thống tạm bận" (BR-U01-95).
 */
@Component
public class RequestThrottle {

    static final String PREFIX = "ratelimit:auth:";

    private final RedisConnectionFactory connectionFactory;
    private final IdentityProperties.RateLimit limits;
    private volatile ProxyManager<byte[]> proxyManager;

    public RequestThrottle(RedisConnectionFactory connectionFactory, IdentityProperties properties) {
        this.connectionFactory = connectionFactory;
        this.limits = properties.rateLimit();
    }

    /**
     * Đăng nhập: IP còn lượt sai không (BR-U01-43, 30 lần sai mỗi 5 phút). Chỉ lần sai mới trừ lượt
     * ({@link #recordLoginFailure}) để nhiều sinh viên đăng nhập đúng qua chung một NAT của trường không bị chặn.
     */
    public boolean loginAllowed(String clientIp) {
        return available("login-ip:" + clientIp, loginBandwidth());
    }

    public void recordLoginFailure(String clientIp) {
        consume("login-ip:" + clientIp, loginBandwidth());
    }

    /** Đổi mật khẩu: tài khoản còn lượt nhập sai mật khẩu hiện tại không (mặc định 5 lần mỗi 15 phút). */
    public boolean passwordChangeAllowed(UUID accountId) {
        return available("password-change:" + accountId, passwordChangeBandwidth());
    }

    public void recordPasswordChangeFailure(UUID accountId) {
        consume("password-change:" + accountId, passwordChangeBandwidth());
    }

    private Bandwidth loginBandwidth() {
        return bandwidth(limits.loginPerIp(), limits.loginIpWindow());
    }

    private Bandwidth passwordChangeBandwidth() {
        return bandwidth(limits.passwordChangeFailures(), limits.passwordChangeWindow());
    }

    /** Yêu cầu OTP: 5 lần/giờ và cách nhau tối thiểu 60 giây mỗi email; 20 lần/giờ mỗi IP (BR-U01-25). */
    public boolean tryOtpRequest(String normalizedEmail, String clientIp) {
        boolean ipAllowed = tryConsume("otp-ip:" + clientIp,
                bandwidth(limits.otpPerIpPerHour(), Duration.ofHours(1)));
        if (!ipAllowed) {
            return false;
        }
        return tryConsume("otp-email:" + Hashing.sha256Hex(normalizedEmail),
                bandwidth(limits.otpPerEmailPerHour(), Duration.ofHours(1)),
                bandwidth(1, limits.otpCooldown()));
    }

    static Bandwidth bandwidth(int capacity, Duration period) {
        return Bandwidth.builder().capacity(capacity).refillIntervally(capacity, period).build();
    }

    private boolean tryConsume(String key, Bandwidth... bandwidths) {
        return redisCall(() -> bucket(key, bandwidths).tryConsume(1));
    }

    /** Ghi một lượt; hết lượt thì thôi (không chờ). */
    private void consume(String key, Bandwidth... bandwidths) {
        redisCall(() -> bucket(key, bandwidths).tryConsume(1));
    }

    private boolean available(String key, Bandwidth... bandwidths) {
        return redisCall(() -> bucket(key, bandwidths).getAvailableTokens() > 0);
    }

    private Bucket bucket(String key, Bandwidth... bandwidths) {
        ConfigurationBuilder builder = BucketConfiguration.builder();
        for (Bandwidth bandwidth : bandwidths) {
            builder.addLimit(bandwidth);
        }
        BucketConfiguration configuration = builder.build();
        return proxyManager().builder().build((PREFIX + key).getBytes(StandardCharsets.UTF_8), () -> configuration);
    }

    private static boolean redisCall(BooleanSupplier call) {
        try {
            return call.getAsBoolean();
        } catch (RuntimeException e) {
            // Lỗi của Lettuce đổi sang lỗi Redis của Spring để xử lý chung một chỗ (503, BR-U01-95).
            throw new RedisConnectionFailureException("Không kiểm được giới hạn tần suất", e);
        }
    }

    private ProxyManager<byte[]> proxyManager() {
        ProxyManager<byte[]> manager = proxyManager;
        if (manager == null) {
            synchronized (this) {
                if (proxyManager == null) {
                    RedisClient client = (RedisClient) ((LettuceConnectionFactory) connectionFactory)
                            .getRequiredNativeClient();
                    proxyManager = Bucket4jLettuce.casBasedBuilder(client)
                            .expirationAfterWrite(ExpirationAfterWriteStrategy
                                    .basedOnTimeForRefillingBucketUpToMax(Duration.ofSeconds(10)))
                            .build();
                }
                manager = proxyManager;
            }
        }
        return manager;
    }
}
