package edu.aiplatform.ai.infrastructure;

import edu.aiplatform.ai.application.IMcpRateLimiter;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.lettuce.core.RedisClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.stereotype.Component;

/**
 * Đếm lời gọi MCP theo credential bằng Bucket4j trên Redis (tech-stack U06). Redis không là nguồn quota; Redis lỗi
 * thì từ chối lời gọi (fail closed, BR-U06-53).
 */
@Component
public class RedisMcpRateLimiter implements IMcpRateLimiter {

    static final String PREFIX = "ratelimit:mcp:";
    static final int PER_MINUTE = 60;

    private final RedisConnectionFactory connectionFactory;
    private volatile ProxyManager<byte[]> proxyManager;

    public RedisMcpRateLimiter(RedisConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public boolean tryAcquire(UUID credentialId) {
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.builder().capacity(PER_MINUTE).refillIntervally(PER_MINUTE, Duration.ofMinutes(1))
                        .build())
                .build();
        try {
            return proxyManager().builder()
                    .build((PREFIX + credentialId).getBytes(StandardCharsets.UTF_8), () -> configuration)
                    .tryConsume(1);
        } catch (RuntimeException e) {
            throw new RedisConnectionFailureException("Không kiểm được giới hạn lời gọi MCP", e);
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
