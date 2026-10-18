package edu.aiplatform.identity.infrastructure;

import edu.aiplatform.identity.application.IdentityProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConfigurationBuilder;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.lettuce.core.RedisClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
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

    /** Đăng nhập: 30 lần mỗi 5 phút cho mỗi IP (BR-U01-43). */
    public boolean tryLogin(String clientIp) {
        return tryConsume("login-ip:" + clientIp,
                bandwidth(limits.loginPerIp(), limits.loginIpWindow()));
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
        ConfigurationBuilder builder = BucketConfiguration.builder();
        for (Bandwidth bandwidth : bandwidths) {
            builder.addLimit(bandwidth);
        }
        BucketConfiguration configuration = builder.build();
        try {
            return proxyManager().builder()
                    .build((PREFIX + key).getBytes(StandardCharsets.UTF_8), () -> configuration)
                    .tryConsume(1);
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
