package edu.aiplatform.identity.infrastructure;

import static edu.aiplatform.identity.IdentityTestData.NOW;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import edu.aiplatform.identity.IdentityTestData;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SessionStoreTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
    private final UUID accountId = UUID.randomUUID();
    private FakeRedis redis;
    private MutableClock clock;
    private SessionStore store;

    @BeforeEach
    void setUp() {
        redis = new FakeRedis();
        clock = new MutableClock(NOW);
        store = new SessionStore(redis.template, objectMapper, clock, IdentityTestData.properties());
    }

    @Test
    void createdSessionIsFoundAndRefreshTokenIsStoredHashed() {
        SessionStore.IssuedSession session = store.create(accountId);

        assertThat(store.find(session.sid())).hasValueSatisfying(s -> assertThat(s.accountId()).isEqualTo(accountId));
        assertThat(redis.values).doesNotContainValue(session.refreshToken());
        assertThat(redis.values).containsKey(SessionStore.REFRESH_PREFIX + Hashing.sha256Hex(session.refreshToken()));
        assertThat(redis.ttls.get(SessionStore.SESSION_PREFIX + session.sid())).isEqualTo(Duration.ofHours(2));
    }

    @Test
    void rotateIssuesNewTokenAndReuseOfOldTokenRevokesSession() {
        SessionStore.IssuedSession session = store.create(accountId);

        SessionStore.IRotateResult first = store.rotate(session.refreshToken());
        assertThat(first).isInstanceOf(SessionStore.Rotated.class);
        String newToken = ((SessionStore.Rotated) first).refreshToken();
        assertThat(newToken).isNotEqualTo(session.refreshToken());

        // BR-U01-46: token cũ bị dùng lại (quá khoảng refresh đồng thời) thì thu hồi cả phiên, token mới cũng mất hiệu lực.
        clock.set(NOW.plus(SessionStore.REUSE_GRACE).plusSeconds(1));
        assertThat(store.rotate(session.refreshToken()))
                .isEqualTo(new SessionStore.Rejected(SessionStore.Reason.REUSED));
        assertThat(store.find(session.sid())).isEmpty();
        assertThat(store.rotate(newToken)).isInstanceOf(SessionStore.Rejected.class);
    }

    /** Hai tab refresh cùng lúc: bên thua bị từ chối nhưng phiên và token mới của bên thắng vẫn dùng được. */
    @Test
    void concurrentRefreshWithinGraceKeepsSession() {
        SessionStore.IssuedSession session = store.create(accountId);
        String newToken = ((SessionStore.Rotated) store.rotate(session.refreshToken())).refreshToken();

        clock.set(NOW.plus(SessionStore.REUSE_GRACE));
        assertThat(store.rotate(session.refreshToken()))
                .isEqualTo(new SessionStore.Rejected(SessionStore.Reason.CONCURRENT));

        assertThat(store.find(session.sid())).isPresent();
        assertThat(store.rotate(newToken)).isInstanceOf(SessionStore.Rotated.class);
    }

    @Test
    void legacyUsedMarkWithoutTimestampCountsAsReuse() {
        SessionStore.IssuedSession session = store.create(accountId);
        String key = SessionStore.REFRESH_PREFIX + Hashing.sha256Hex(session.refreshToken());
        redis.values.put(key, SessionStore.USED_MARK + session.sid());

        assertThat(store.rotate(session.refreshToken()))
                .isEqualTo(new SessionStore.Rejected(SessionStore.Reason.REUSED));
        assertThat(store.find(session.sid())).isEmpty();
    }

    @Test
    void sidOfReturnsSessionOnlyForLiveRefreshToken() {
        SessionStore.IssuedSession session = store.create(accountId);
        assertThat(store.sidOf(session.refreshToken())).contains(session.sid());

        String newToken = ((SessionStore.Rotated) store.rotate(session.refreshToken())).refreshToken();
        assertThat(store.sidOf(session.refreshToken())).isEmpty();
        assertThat(store.sidOf(newToken)).contains(session.sid());
        assertThat(store.sidOf(null)).isEmpty();
    }

    @Test
    void sessionEndsAfterSevenDaysEvenWhenUsed() {
        SessionStore.IssuedSession session = store.create(accountId);
        clock.set(NOW.plus(Duration.ofDays(7)));

        assertThat(store.rotate(session.refreshToken()))
                .isEqualTo(new SessionStore.Rejected(SessionStore.Reason.EXPIRED));
        assertThat(store.find(session.sid())).isEmpty();
    }

    @Test
    void rotationTtlNeverPassesSevenDayCeiling() {
        SessionStore.IssuedSession session = store.create(accountId);
        clock.set(NOW.plus(Duration.ofDays(7)).minus(Duration.ofMinutes(30)));

        store.rotate(session.refreshToken());

        assertThat(redis.ttls.get(SessionStore.SESSION_PREFIX + session.sid())).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    void unknownOrBlankTokenIsRejected() {
        assertThat(store.rotate("khong-ton-tai")).isEqualTo(new SessionStore.Rejected(SessionStore.Reason.UNKNOWN));
        assertThat(store.rotate(" ")).isEqualTo(new SessionStore.Rejected(SessionStore.Reason.UNKNOWN));
    }

    @Test
    void logoutRemovesOnlyThatSession() {
        SessionStore.IssuedSession a = store.create(accountId);
        SessionStore.IssuedSession b = store.create(accountId);

        store.delete(a.sid());

        assertThat(store.find(a.sid())).isEmpty();
        assertThat(store.find(b.sid())).isPresent();
    }

    @Test
    void deleteAllExceptKeepsCurrentSession() {
        SessionStore.IssuedSession current = store.create(accountId);
        SessionStore.IssuedSession other = store.create(accountId);
        SessionStore.IssuedSession someoneElse = store.create(UUID.randomUUID());

        store.deleteAllExcept(accountId, current.sid());
        assertThat(store.find(current.sid())).isPresent();
        assertThat(store.find(other.sid())).isEmpty();

        store.deleteAll(accountId);
        assertThat(store.find(current.sid())).isEmpty();
        assertThat(store.find(someoneElse.sid())).isPresent();
    }

    /** Đồng hồ đặt lại được trong test. */
    static final class MutableClock extends Clock {

        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void set(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
