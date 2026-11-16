package edu.aiplatform.identity.integration;

import static edu.aiplatform.identity.IdentityTestData.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
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
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
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
    void injectedMapperSupportsLegacyJsonAndRollbackReaderIgnoresStamp() {
        new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
                .withBean(StringRedisTemplate.class, () -> redis)
                .withBean(java.time.Clock.class, () -> clock)
                .withBean(edu.aiplatform.identity.config.IdentityProperties.class, IdentityTestData::properties)
                .withBean(SessionStore.class)
                .run(context -> {
                    ObjectMapper injected = context.getBean(ObjectMapper.class);
                    assertThat(injected.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)).isFalse();
                    SessionStore injectedStore = context.getBean(SessionStore.class);
                    var issued = injectedStore.create(accountId, NOW.toEpochMilli());
                    String key = SessionStore.SESSION_PREFIX + issued.sid();
                    String json = redis.opsForValue().get(key);
                    assertThat(injected.readValue(json, LegacySessionRecord.class).accountId()).isEqualTo(accountId);
                    var legacy = injected.readTree(json);
                    ((com.fasterxml.jackson.databind.node.ObjectNode) legacy).remove("pwdStamp");
                    redis.opsForValue().set(key, injected.writeValueAsString(legacy));
                    assertThat(injectedStore.find(issued.sid())).hasValueSatisfying(session -> {
                        assertThat(session.pwdStamp()).isNull();
                        assertThat(session.isPasswordCurrent(NOW)).isTrue();
                        assertThat(session.isPasswordCurrent(NOW.plusNanos(1))).isFalse();
                    });
                });
    }

    record LegacySessionRecord(UUID accountId, java.time.Instant issuedAt, java.time.Instant lastUsedAt,
                               String refreshHash) {
    }

    @Test
    void concurrentObserverNeverSeesPartiallyCreatedSessions() throws Exception {
        var snapshot = new DefaultRedisScript<>("""
                for _, key in ipairs(redis.call('KEYS', 'session:*')) do
                    if string.sub(key, 1, 16) ~= 'session:refresh:' then
                        local session = cjson.decode(redis.call('GET', key))
                        local sid = string.sub(key, 9)
                        if redis.call('SISMEMBER', 'account:sessions:' .. session.accountId, sid) ~= 1
                            or redis.call('GET', 'session:refresh:' .. session.refreshHash) ~= sid
                            or redis.call('PTTL', key) <= 0 then return 0 end
                    end
                end
                return 1
                """, Long.class);
        CountDownLatch start = new CountDownLatch(1);
        var pool = Executors.newSingleThreadExecutor();
        try {
            var writer = pool.submit(() -> {
                start.await();
                for (int i = 0; i < 50; i++) {
                    store.create(accountId, NOW.toEpochMilli());
                }
                return null;
            });
            start.countDown();
            for (int i = 0; i < 50; i++) {
                assertThat(redis.execute(snapshot, List.of())).isEqualTo(1L);
            }
            writer.get(10, TimeUnit.SECONDS);
            assertThat(redis.opsForSet().size(SessionStore.ACCOUNT_SESSIONS_PREFIX + accountId)).isEqualTo(50);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void staleRotationCasCannotRestoreDeletedSession() {
        assertStaleRotationRejected(false);
    }

    @Test
    void staleRotationCasCannotRestoreBulkRevokedSession() {
        assertStaleRotationRejected(true);
    }

    private void assertStaleRotationRejected(boolean bulk) {
        SessionStore.IssuedSession session = store.create(accountId, NOW.toEpochMilli());
        String key = SessionStore.SESSION_PREFIX + session.sid();
        String expectedJson = redis.opsForValue().get(key);
        if (bulk) {
            store.deleteAll(accountId);
        } else {
            store.delete(session.sid());
        }
        String newRefreshKey = SessionStore.REFRESH_PREFIX + "new-hash";

        assertThat(redis.execute(SessionStore.ROTATE_CAS, List.of(key, newRefreshKey),
                expectedJson, expectedJson, "1000", session.sid())).isZero();
        assertThat(redis.hasKey(key)).isFalse();
        assertThat(redis.hasKey(newRefreshKey)).isFalse();
        assertThat(redis.opsForSet().members(SessionStore.ACCOUNT_SESSIONS_PREFIX + accountId)).isEmpty();
    }

    @Test
    void deletionBetweenRotationReadAndCasCannotResurrectSession() throws Exception {
        assertRevocationDuringRotation(false);
    }

    @Test
    void bulkRevocationBetweenRotationReadAndCasCannotResurrectSession() throws Exception {
        assertRevocationDuringRotation(true);
    }

    private void assertRevocationDuringRotation(boolean bulk) throws Exception {
        SessionStore.IssuedSession session = store.create(accountId, NOW.toEpochMilli());
        CountDownLatch read = new CountDownLatch(1);
        CountDownLatch resume = new CountDownLatch(1);
        SessionStore paused = new SessionStore(redis, objectMapper, clock, IdentityTestData.properties()) {
            @Override
            void afterRead() {
                read.countDown();
                try {
                    assertThat(resume.await(10, TimeUnit.SECONDS)).isTrue();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(e);
                }
            }
        };
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<SessionStore.IRotateResult> rotation = pool.submit(() -> paused.rotate(session.refreshToken()));
            assertThat(read.await(10, TimeUnit.SECONDS)).isTrue();
            if (bulk) {
                store.deleteAll(accountId);
            } else {
                store.delete(session.sid());
            }
            resume.countDown();
            assertThat(rotation.get(10, TimeUnit.SECONDS))
                    .isEqualTo(new SessionStore.Rejected(SessionStore.Reason.UNKNOWN));
            assertThat(redis.hasKey(SessionStore.SESSION_PREFIX + session.sid())).isFalse();
            assertThat(redis.keys(SessionStore.REFRESH_PREFIX + "*")).isEmpty();
            assertThat(redis.opsForSet().members(SessionStore.ACCOUNT_SESSIONS_PREFIX + accountId)).isEmpty();
        } finally {
            resume.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    void rotateThenDeleteRemovesCurrentRefreshAndIndex() {
        SessionStore.IssuedSession session = store.create(accountId, NOW.toEpochMilli());
        SessionStore.Rotated rotated = (SessionStore.Rotated) store.rotate(session.refreshToken());

        store.delete(session.sid());

        assertThat(redis.hasKey(SessionStore.SESSION_PREFIX + session.sid())).isFalse();
        assertThat(redis.hasKey(refreshKey(rotated.refreshToken()))).isFalse();
        assertThat(redis.opsForSet().members(SessionStore.ACCOUNT_SESSIONS_PREFIX + accountId)).isEmpty();
    }

    @Test
    void restampPreservesTtlAndCannotRestoreDeletedOrChangedSession() {
        SessionStore.IssuedSession session = store.create(accountId, NOW.toEpochMilli());
        String key = SessionStore.SESSION_PREFIX + session.sid();
        String oldJson = redis.opsForValue().get(key);
        Long ttl = redis.getExpire(key, TimeUnit.MILLISECONDS);

        store.restamp(session.sid(), NOW.plusMillis(1).toEpochMilli());

        assertThat(store.find(session.sid())).hasValueSatisfying(s ->
                assertThat(s.pwdStamp()).isEqualTo(NOW.plusMillis(1).toEpochMilli()));
        assertThat(redis.getExpire(key, TimeUnit.MILLISECONDS)).isBetween(ttl - 5000, ttl);
        assertThat(redis.execute(SessionStore.RESTAMP_CAS, List.of(key), oldJson, oldJson)).isZero();
        store.delete(session.sid());
        store.restamp(session.sid(), NOW.plusMillis(2).toEpochMilli());
        assertThat(redis.execute(SessionStore.RESTAMP_CAS, List.of(key), oldJson, oldJson)).isZero();
        assertThat(redis.hasKey(key)).isFalse();
        assertThat(redis.hasKey(refreshKey(session.refreshToken()))).isFalse();
        assertThat(redis.opsForSet().members(SessionStore.ACCOUNT_SESSIONS_PREFIX + accountId)).isEmpty();
    }

    @Test
    void corruptJsonCanStillBeDeleted() {
        redis.opsForValue().set(SessionStore.SESSION_PREFIX + "broken", "{invalid");
        store.delete("broken");
        assertThat(redis.hasKey(SessionStore.SESSION_PREFIX + "broken")).isFalse();
    }

    @Test
    void createWritesHashedRefreshKeyWithIdleTtlAndAccountIndexWithMaxTtl() {
        SessionStore.IssuedSession session = store.create(accountId, NOW.toEpochMilli());
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
        SessionStore.IssuedSession session = store.create(accountId, NOW.toEpochMilli());
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
        SessionStore.IssuedSession session = store.create(accountId, NOW.toEpochMilli());
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
        SessionStore.IssuedSession session = store.create(accountId, NOW.toEpochMilli());

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
        SessionStore.IssuedSession session = store.create(accountId, NOW.toEpochMilli());

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
        SessionStore.IssuedSession session = store.create(accountId, NOW.toEpochMilli());
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
        SessionStore.IssuedSession keep = store.create(accountId, NOW.toEpochMilli());
        SessionStore.IssuedSession other = store.create(accountId, NOW.toEpochMilli());
        SessionStore.IssuedSession third = store.create(accountId, NOW.toEpochMilli());
        SessionStore.IssuedSession otherAccount = store.create(UUID.randomUUID(), NOW.toEpochMilli());

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

    @Test
    void refreshRacingPasswordRestampKeepsAUsableRefreshToken() {
        var issued = store.create(accountId, NOW.toEpochMilli());
        SessionStore overlapping = new SessionStore(redis, objectMapper, clock, IdentityTestData.properties()) {
            @Override
            void afterRead() {
                // Deterministic interleave: password change restamps after refresh read, before its CAS.
                store.restamp(issued.sid(), NOW.plusMillis(1).toEpochMilli());
            }
        };

        var result = overlapping.rotate(issued.refreshToken());

        assertThat(store.find(issued.sid())).isPresent();
        assertThat(store.sidOf(issued.refreshToken())).isEmpty();
        assertThat(redis.opsForValue().get(refreshKey(issued.refreshToken())))
                .startsWith(SessionStore.USED_MARK);
        assertThat(result).as("the session kept by password change must remain refreshable")
                .isInstanceOf(SessionStore.Rotated.class);
        var rotated = (SessionStore.Rotated) result;
        assertThat(rotated.session().pwdStamp()).isEqualTo(NOW.plusMillis(1).toEpochMilli());
        assertThat(store.sidOf(rotated.refreshToken())).hasValue(issued.sid());
        assertThat(store.rotate(rotated.refreshToken())).isInstanceOf(SessionStore.Rotated.class);
    }

    @Test
    void restampRacingRefreshDoesNotSilentlyLeaveAnOldPasswordStamp() {
        var issued = store.create(accountId, NOW.toEpochMilli());
        var interleavingRedis = new StringRedisTemplate(connectionFactory) {
            @Override
            public <T> T execute(org.springframework.data.redis.core.script.RedisScript<T> script,
                                 List<String> keys, Object... args) {
                if (script == SessionStore.RESTAMP_CAS) store.rotate(issued.refreshToken());
                return super.execute(script, keys, args);
            }
        };
        var overlapping = new SessionStore(interleavingRedis, objectMapper, clock, IdentityTestData.properties());

        overlapping.restamp(issued.sid(), NOW.plusMillis(1).toEpochMilli());

        assertThat(store.find(issued.sid())).hasValueSatisfying(session ->
                assertThat(session.isPasswordCurrent(NOW.plusMillis(1)))
                        .as("the kept session must accept the committed password generation").isTrue());
        assertThat(store.find(issued.sid())).hasValueSatisfying(session ->
                assertThat(redis.opsForValue().get(SessionStore.REFRESH_PREFIX + session.refreshHash()))
                        .isEqualTo(issued.sid()));
    }

    @Test
    void corruptJsonNullIsRejectedWithoutCrashing() {
        var issued = store.create(accountId, NOW.toEpochMilli());
        String key = SessionStore.SESSION_PREFIX + issued.sid();
        redis.opsForValue().set(key, "null");

        assertThat(store.find(issued.sid())).isEmpty();
        assertThat(redis.hasKey(key)).isFalse();
    }

    @Test
    void incompleteSessionRecordsAreDeletedBeforeUse() throws Exception {
        for (String field : List.of("accountId", "issuedAt", "lastUsedAt", "refreshHash")) {
            var issued = store.create(accountId, NOW.toEpochMilli());
            String key = SessionStore.SESSION_PREFIX + issued.sid();
            var json = (com.fasterxml.jackson.databind.node.ObjectNode)
                    objectMapper.readTree(redis.opsForValue().get(key));
            json.remove(field);
            redis.opsForValue().set(key, objectMapper.writeValueAsString(json));

            assertThat(store.find(issued.sid())).as("missing %s", field).isEmpty();
            assertThat(redis.hasKey(key)).isFalse();
        }
        var issued = store.create(accountId, NOW.toEpochMilli());
        String key = SessionStore.SESSION_PREFIX + issued.sid();
        redis.opsForValue().set(key, objectMapper.writeValueAsString(
                new SessionStore.SessionRecord(accountId, NOW, NOW, " ", null)));
        assertThat(store.find(issued.sid())).isEmpty();
        assertThat(redis.hasKey(key)).isFalse();
    }

    @Test
    void repeatedRestampsBoundRotationRetriesWithoutPublishingANewRefresh() {
        var issued = store.create(accountId, NOW.toEpochMilli());
        AtomicInteger attempts = new AtomicInteger();
        var overlapping = new SessionStore(redis, objectMapper, clock, IdentityTestData.properties()) {
            @Override
            void afterRead() {
                store.restamp(issued.sid(), NOW.toEpochMilli() + attempts.incrementAndGet());
            }
        };

        assertThat(overlapping.rotate(issued.refreshToken()))
                .isEqualTo(new SessionStore.Rejected(SessionStore.Reason.UNKNOWN));
        assertThat(attempts.get()).isEqualTo(3);
        assertThat(redis.keys(SessionStore.REFRESH_PREFIX + "*"))
                .containsExactly(refreshKey(issued.refreshToken()));
    }

    @Test
    void repeatedRestampConflictsAreReportedAfterThreeAttempts() {
        var issued = store.create(accountId, NOW.toEpochMilli());
        AtomicInteger attempts = new AtomicInteger();
        var interleavingRedis = new StringRedisTemplate(connectionFactory) {
            @Override
            public <T> T execute(org.springframework.data.redis.core.script.RedisScript<T> script,
                                 List<String> keys, Object... args) {
                if (script == SessionStore.RESTAMP_CAS) {
                    store.restamp(issued.sid(), NOW.toEpochMilli() + attempts.incrementAndGet());
                }
                return super.execute(script, keys, args);
            }
        };
        var overlapping = new SessionStore(interleavingRedis, objectMapper, clock, IdentityTestData.properties());

        assertThatThrownBy(() -> overlapping.restamp(issued.sid(), NOW.plusMillis(100).toEpochMilli()))
                .isInstanceOf(IllegalStateException.class);
        assertThat(attempts.get()).isEqualTo(3);
        assertThat(store.find(issued.sid())).hasValueSatisfying(session ->
                assertThat(session.pwdStamp()).isEqualTo(NOW.plusMillis(3).toEpochMilli()));
    }

    @Test
    void restampRetryCannotRestoreConcurrentlyDeletedSession() {
        var issued = store.create(accountId, NOW.toEpochMilli());
        var interleavingRedis = new StringRedisTemplate(connectionFactory) {
            @Override
            public <T> T execute(org.springframework.data.redis.core.script.RedisScript<T> script,
                                 List<String> keys, Object... args) {
                if (script == SessionStore.RESTAMP_CAS) {
                    store.delete(issued.sid());
                }
                return super.execute(script, keys, args);
            }
        };
        var overlapping = new SessionStore(interleavingRedis, objectMapper, clock, IdentityTestData.properties());

        overlapping.restamp(issued.sid(), NOW.plusMillis(1).toEpochMilli());

        assertThat(store.find(issued.sid())).isEmpty();
        assertThat(redis.hasKey(refreshKey(issued.refreshToken()))).isFalse();
        assertThat(redis.opsForSet().members(SessionStore.ACCOUNT_SESSIONS_PREFIX + accountId)).isEmpty();
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
