package edu.aiplatform.identity.integration;

import static edu.aiplatform.identity.IdentityTestData.NOW;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import edu.aiplatform.identity.IdentityTestData;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
 * {@link SessionStore} trên Redis 7 thật: script Lua {@link SessionStore#CLAIM_REFRESH} chạy trên server, TTL do Redis
 * giữ. Đồng hồ của store là giả để kiểm khoảng ân hạn và trần 7 ngày mà không phải chờ.
 */
@Testcontainers(disabledWithoutDocker = true)
class SessionStoreIntegrationTest {

    @Container
    static final GenericContainer<?> REDIS = RedisTestSupport.container();

    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redis;

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
    private final UUID accountId = UUID.randomUUID();
    private SessionStoreTest.MutableClock clock;
    private SessionStore store;

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
        clock = new SessionStoreTest.MutableClock(NOW);
        store = new SessionStore(redis, objectMapper, clock, IdentityTestData.properties());
    }

    @Test
    void createWritesHashedRefreshKeyWithIdleTtlAndAccountIndexWithMaxTtl() {
        SessionStore.IssuedSession session = store.create(accountId);
        String refreshKey = refreshKey(session.refreshToken());

        assertThat(redis.opsForValue().get(refreshKey)).isEqualTo(session.sid());
        assertThat(store.find(session.sid())).hasValueSatisfying(s -> {
            assertThat(s.accountId()).isEqualTo(accountId);
            assertThat(s.issuedAt()).isEqualTo(NOW);
        });
        assertTtl(SessionStore.SESSION_PREFIX + session.sid(), Duration.ofHours(2));
        assertTtl(refreshKey, Duration.ofHours(2));
        assertTtl(SessionStore.ACCOUNT_SESSIONS_PREFIX + accountId, Duration.ofDays(7));
        assertThat(redis.opsForSet().members(SessionStore.ACCOUNT_SESSIONS_PREFIX + accountId))
                .containsExactly(session.sid());
        for (String key : redis.keys("*")) {
            assertThat(key).doesNotContain(session.refreshToken());
            if (!key.startsWith(SessionStore.ACCOUNT_SESSIONS_PREFIX)) {
                assertThat(redis.opsForValue().get(key)).doesNotContain(session.refreshToken());
            }
        }
    }

    /** Nhiều tab refresh cùng lúc bằng một token: script Lua để đúng một request thắng, còn lại CONCURRENT. */
    @Test
    void concurrentRefreshesWithSameTokenRotateExactlyOnce() throws Exception {
        SessionStore.IssuedSession session = store.create(accountId);
        int callers = 8;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(callers);
        List<SessionStore.IRotateResult> results = new ArrayList<>();
        try {
            List<Future<SessionStore.IRotateResult>> futures = new ArrayList<>();
            for (int i = 0; i < callers; i++) {
                Callable<SessionStore.IRotateResult> call = () -> {
                    start.await();
                    return store.rotate(session.refreshToken());
                };
                futures.add(pool.submit(call));
            }
            start.countDown();
            for (Future<SessionStore.IRotateResult> future : futures) {
                results.add(future.get(10, TimeUnit.SECONDS));
            }
        } finally {
            pool.shutdownNow();
        }

        List<SessionStore.Rotated> rotated = results.stream()
                .filter(SessionStore.Rotated.class::isInstance).map(SessionStore.Rotated.class::cast).toList();
        assertThat(rotated).hasSize(1);
        assertThat(results).filteredOn(SessionStore.Rejected.class::isInstance)
                .hasSize(callers - 1)
                .containsOnly(new SessionStore.Rejected(SessionStore.Reason.CONCURRENT));

        // Phiên còn nguyên, token mới của bên thắng dùng tiếp được.
        assertThat(store.find(session.sid())).isPresent();
        assertThat(store.sidOf(rotated.get(0).refreshToken())).hasValue(session.sid());
        assertThat(store.rotate(rotated.get(0).refreshToken())).isInstanceOf(SessionStore.Rotated.class);
    }

    @Test
    void oldTokenWithinGraceIsConcurrentButAfterGraceRevokesSession() {
        SessionStore.IssuedSession session = store.create(accountId);
        String newToken = ((SessionStore.Rotated) store.rotate(session.refreshToken())).refreshToken();
        // Dấu "đã dùng" giữ tới hết idle TTL để còn phát hiện dùng lại.
        assertThat(redis.opsForValue().get(refreshKey(session.refreshToken())))
                .isEqualTo(SessionStore.USED_MARK + session.sid() + ":" + NOW.toEpochMilli());
        assertTtl(refreshKey(session.refreshToken()), Duration.ofHours(2));

        clock.set(NOW.plus(SessionStore.REUSE_GRACE));
        assertThat(store.rotate(session.refreshToken()))
                .isEqualTo(new SessionStore.Rejected(SessionStore.Reason.CONCURRENT));
        assertThat(store.find(session.sid())).isPresent();

        clock.set(NOW.plus(SessionStore.REUSE_GRACE).plusMillis(1));
        assertThat(store.rotate(session.refreshToken()))
                .isEqualTo(new SessionStore.Rejected(SessionStore.Reason.REUSED));
        assertThat(store.find(session.sid())).isEmpty();
        assertThat(redis.hasKey(SessionStore.SESSION_PREFIX + session.sid())).isFalse();
        assertThat(redis.hasKey(refreshKey(newToken))).isFalse();
        assertThat(redis.opsForSet().members(SessionStore.ACCOUNT_SESSIONS_PREFIX + accountId)).isEmpty();
        assertThat(store.rotate(newToken)).isEqualTo(new SessionStore.Rejected(SessionStore.Reason.UNKNOWN));
    }

    @Test
    void rotationShrinksTtlToSevenDayCapAndExpiresAfterIt() {
        SessionStore.IssuedSession session = store.create(accountId);

        clock.set(NOW.plus(Duration.ofDays(7)).minus(Duration.ofMinutes(30)));
        SessionStore.Rotated rotated = (SessionStore.Rotated) store.rotate(session.refreshToken());
        assertTtl(SessionStore.SESSION_PREFIX + session.sid(), Duration.ofMinutes(30));
        assertTtl(refreshKey(rotated.refreshToken()), Duration.ofMinutes(30));
        assertThat(store.find(session.sid())).hasValueSatisfying(s -> assertThat(s.issuedAt()).isEqualTo(NOW));

        clock.set(NOW.plus(Duration.ofDays(7)).plusSeconds(1));
        assertThat(store.rotate(rotated.refreshToken()))
                .isEqualTo(new SessionStore.Rejected(SessionStore.Reason.EXPIRED));
        assertThat(redis.hasKey(SessionStore.SESSION_PREFIX + session.sid())).isFalse();
    }

    @Test
    void findDropsSessionPastSevenDays() {
        SessionStore.IssuedSession session = store.create(accountId);

        clock.set(NOW.plus(Duration.ofDays(7)));
        assertThat(store.find(session.sid())).isEmpty();
        assertThat(redis.hasKey(SessionStore.SESSION_PREFIX + session.sid())).isFalse();
        assertThat(redis.hasKey(refreshKey(session.refreshToken()))).isFalse();
    }

    @Test
    void unknownOrBlankTokenIsRejectedAndClaimScriptReturnsNothing() {
        assertThat(store.rotate("khong-ton-tai")).isEqualTo(new SessionStore.Rejected(SessionStore.Reason.UNKNOWN));
        assertThat(store.rotate(" ")).isEqualTo(new SessionStore.Rejected(SessionStore.Reason.UNKNOWN));
        assertThat(redis.execute(SessionStore.CLAIM_REFRESH, List.of(SessionStore.REFRESH_PREFIX + "x"), "1", "1000"))
                .isNull();
        assertThat(redis.hasKey(SessionStore.REFRESH_PREFIX + "x")).isFalse();
    }

    @Test
    void sidOfFollowsOnlyLiveRefreshTokens() {
        SessionStore.IssuedSession session = store.create(accountId);
        assertThat(store.sidOf(session.refreshToken())).hasValue(session.sid());

        String newToken = ((SessionStore.Rotated) store.rotate(session.refreshToken())).refreshToken();
        assertThat(store.sidOf(session.refreshToken())).isEmpty();
        assertThat(store.sidOf(newToken)).hasValue(session.sid());
        assertThat(store.sidOf("khong-ton-tai")).isEmpty();
        assertThat(store.sidOf(null)).isEmpty();

        store.delete(session.sid());
        assertThat(store.sidOf(newToken)).isEmpty();
    }

    @Test
    void deleteAllExceptKeepsOnlyCurrentSessionAndDeleteAllRemovesEverything() {
        SessionStore.IssuedSession keep = store.create(accountId);
        SessionStore.IssuedSession other = store.create(accountId);
        SessionStore.IssuedSession third = store.create(accountId);
        SessionStore.IssuedSession otherAccount = store.create(UUID.randomUUID());

        store.deleteAllExcept(accountId, keep.sid());

        assertThat(store.find(keep.sid())).isPresent();
        assertThat(store.find(other.sid())).isEmpty();
        assertThat(store.find(third.sid())).isEmpty();
        assertThat(redis.hasKey(refreshKey(other.refreshToken()))).isFalse();
        assertThat(redis.hasKey(refreshKey(third.refreshToken()))).isFalse();
        assertThat(redis.opsForSet().members(SessionStore.ACCOUNT_SESSIONS_PREFIX + accountId))
                .isEqualTo(Set.of(keep.sid()));
        assertThat(store.rotate(other.refreshToken())).isEqualTo(new SessionStore.Rejected(SessionStore.Reason.UNKNOWN));

        store.deleteAll(accountId);
        assertThat(store.find(keep.sid())).isEmpty();
        assertThat(redis.hasKey(refreshKey(keep.refreshToken()))).isFalse();
        assertThat(store.find(otherAccount.sid())).isPresent();
        store.deleteAll(UUID.randomUUID());
    }

    private static String refreshKey(String refreshToken) {
        return SessionStore.REFRESH_PREFIX + Hashing.sha256Hex(refreshToken);
    }

    /** TTL thật trên Redis: không quá {@code expected} và chỉ hụt vài giây do thời gian chạy test. */
    private static void assertTtl(String key, Duration expected) {
        Long millis = redis.getExpire(key, TimeUnit.MILLISECONDS);
        assertThat(millis).as("TTL của %s", key)
                .isBetween(expected.minusSeconds(5).toMillis(), expected.toMillis());
    }
}
