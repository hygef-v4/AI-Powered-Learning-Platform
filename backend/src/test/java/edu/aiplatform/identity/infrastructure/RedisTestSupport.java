package edu.aiplatform.identity.infrastructure;

import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/** Redis thật cho test tích hợp U01: cùng bản Redis với {@code infra/docker-compose.yml}. */
final class RedisTestSupport {

    private RedisTestSupport() {
    }

    static GenericContainer<?> container() {
        return new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine")).withExposedPorts(6379);
    }

    static LettuceConnectionFactory connectionFactory(GenericContainer<?> redis) {
        LettuceConnectionFactory factory = new LettuceConnectionFactory(
                new RedisStandaloneConfiguration(redis.getHost(), redis.getMappedPort(6379)));
        factory.afterPropertiesSet();
        factory.start();
        return factory;
    }

    static void flushAll(StringRedisTemplate template) {
        template.execute(connection -> {
            connection.serverCommands().flushAll();
            return null;
        }, true);
    }
}
