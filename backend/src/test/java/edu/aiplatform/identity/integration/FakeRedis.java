package edu.aiplatform.identity.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

/** {@link StringRedisTemplate} giả trong bộ nhớ cho các lệnh U01 dùng; ghi lại TTL để test kiểm. */
public final class FakeRedis {

    final Map<String, String> values = new HashMap<>();
    final Map<String, Duration> ttls = new HashMap<>();
    final Map<String, Set<String>> sets = new HashMap<>();
    final Map<String, Map<Object, Object>> hashes = new HashMap<>();
    public final StringRedisTemplate template = mock(StringRedisTemplate.class);

    @SuppressWarnings("unchecked")
    public FakeRedis() {
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        SetOperations<String, String> setOps = mock(SetOperations.class);
        when(template.opsForValue()).thenReturn(valueOps);
        when(template.opsForSet()).thenReturn(setOps);

        when(valueOps.get(anyString())).thenAnswer(i -> values.get(i.<String>getArgument(0)));
        when(valueOps.getAndDelete(anyString())).thenAnswer(i -> values.remove(i.<String>getArgument(0)));
        org.mockito.Mockito.doAnswer(i -> {
            values.put(i.getArgument(0), i.getArgument(1));
            ttls.put(i.getArgument(0), i.getArgument(2));
            return null;
        }).when(valueOps).set(anyString(), anyString(), any(Duration.class));
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenAnswer(i -> {
            if (values.containsKey(i.<String>getArgument(0))) {
                return false;
            }
            values.put(i.getArgument(0), i.getArgument(1));
            ttls.put(i.getArgument(0), i.getArgument(2));
            return true;
        });

        when(template.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenAnswer(i -> {
            RedisScript<?> script = i.getArgument(0);
            List<String> keys = i.getArgument(1);
            String key = keys.get(0);
            if (script == OtpChallengeStore.WRITE) {
                Map<Object, Object> hash = new HashMap<>();
                hash.put(OtpChallengeStore.CODE_HASH, i.getArgument(2));
                hash.put(OtpChallengeStore.ATTEMPTS_LEFT, i.getArgument(3));
                if (!"".equals(i.getArgument(5))) {
                    hash.put(OtpChallengeStore.JOB, i.getArgument(5));
                }
                hashes.put(key, hash);
                ttls.put(key, Duration.ofMillis(Long.parseLong(i.getArgument(4))));
                return 1L;
            }
            if (i.getArgument(0) == OtpChallengeStore.VERIFY) {
                // OtpChallengeStore.VERIFY: trừ lượt trước rồi so mã, một bước.
                Map<Object, Object> hash = hashes.get(key);
                if (hash == null || hash.get(OtpChallengeStore.CODE_HASH) == null) {
                    return 0L;
                }
                long left = Long.parseLong(String.valueOf(hash.getOrDefault(OtpChallengeStore.ATTEMPTS_LEFT, "0"))) - 1;
                hash.put(OtpChallengeStore.ATTEMPTS_LEFT, String.valueOf(left));
                if (left < 0) {
                    hashes.remove(key);
                    return 0L;
                }
                if (hash.get(OtpChallengeStore.CODE_HASH).equals(i.getArgument(2))) {
                    hashes.remove(key);
                    return 1L;
                }
                if (left <= 0) {
                    hashes.remove(key);
                }
                return 0L;
            }
            if (script == SessionStore.CLAIM_REFRESH) {
                String value = values.get(key);
                if (value == null || value.startsWith(SessionStore.USED_MARK)) {
                    return value;
                }
                values.put(key, SessionStore.USED_MARK + value + ":" + i.getArgument(2));
                ttls.put(key, Duration.ofMillis(Long.parseLong(i.getArgument(3))));
                return value;
            }
            if (script == SessionStore.CREATE) {
                Duration idle = Duration.ofMillis(Long.parseLong(i.getArgument(4)));
                values.put(key, i.getArgument(2));
                values.put(keys.get(1), i.getArgument(3));
                ttls.put(key, idle);
                ttls.put(keys.get(1), idle);
                sets.computeIfAbsent(keys.get(2), k -> new HashSet<>()).add(i.getArgument(3));
                ttls.put(keys.get(2), Duration.ofMillis(Long.parseLong(i.getArgument(5))));
                return 1L;
            }
            if (script == SessionStore.DELETE_SESSION) {
                String json = values.remove(key);
                ttls.remove(key);
                if (json == null) {
                    return 0L;
                }
                try {
                    var session = JsonMapper.builder().findAndAddModules().build()
                            .readValue(json, SessionStore.SessionRecord.class);
                    if (session == null) {
                        return 1L;
                    }
                    String refreshKey = SessionStore.REFRESH_PREFIX + session.refreshHash();
                    values.remove(refreshKey);
                    ttls.remove(refreshKey);
                    Set<String> sids = sets.get(SessionStore.ACCOUNT_SESSIONS_PREFIX + session.accountId());
                    if (sids != null) {
                        sids.remove(i.getArgument(2));
                    }
                } catch (com.fasterxml.jackson.core.JsonProcessingException ignored) {
                    // Corrupt JSON still loses the session key, just like DELETE_SESSION.
                }
                return 1L;
            }
            if (script == SessionStore.ROTATE_CAS || script == SessionStore.RESTAMP_CAS) {
                if (!Objects.equals(values.get(key), i.getArgument(2))) {
                    return 0L;
                }
                values.put(key, i.getArgument(3));
                if (script == SessionStore.ROTATE_CAS) {
                    Duration ttl = Duration.ofMillis(Long.parseLong(i.getArgument(4)));
                    ttls.put(key, ttl);
                    values.put(keys.get(1), i.getArgument(5));
                    ttls.put(keys.get(1), ttl);
                }
                return 1L;
            }
            throw new AssertionError("Unexpected Redis script: " + script.getScriptAsString());
        });

        when(template.delete(anyString())).thenAnswer(i -> values.remove(i.<String>getArgument(0)) != null
                | sets.remove(i.<String>getArgument(0)) != null
                | hashes.remove(i.<String>getArgument(0)) != null);

        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        when(template.opsForHash()).thenReturn(hashOps);
        org.mockito.Mockito.doAnswer(i -> {
            hashes.computeIfAbsent(i.getArgument(0), k -> new HashMap<>()).putAll(i.getArgument(1));
            return null;
        }).when(hashOps).putAll(anyString(), any(Map.class));
        when(hashOps.get(anyString(), any())).thenAnswer(i -> {
            Map<Object, Object> hash = hashes.get(i.<String>getArgument(0));
            return hash == null ? null : hash.get(i.getArgument(1));
        });
        when(hashOps.increment(anyString(), any(), org.mockito.ArgumentMatchers.anyLong())).thenAnswer(i -> {
            Map<Object, Object> hash = hashes.computeIfAbsent(i.getArgument(0), k -> new HashMap<>());
            long next = Long.parseLong(String.valueOf(hash.getOrDefault(i.getArgument(1), "0")))
                    + i.<Long>getArgument(2);
            hash.put(i.getArgument(1), String.valueOf(next));
            return next;
        });
        when(template.expire(anyString(), any(Duration.class))).thenReturn(true);

        when(setOps.add(anyString(), any(String[].class))).thenAnswer(i -> {
            Set<String> set = sets.computeIfAbsent(i.getArgument(0), k -> new HashSet<>());
            long added = 0;
            for (int k = 1; k < i.getArguments().length; k++) {
                added += set.add((String) i.getArgument(k)) ? 1 : 0;
            }
            return added;
        });
        when(setOps.remove(anyString(), any(Object[].class))).thenAnswer(i -> {
            Set<String> set = sets.get(i.<String>getArgument(0));
            long removed = 0;
            for (int k = 1; k < i.getArguments().length; k++) {
                removed += set != null && set.remove(i.getArgument(k)) ? 1 : 0;
            }
            return removed;
        });
        when(setOps.members(anyString())).thenAnswer(i -> {
            Set<String> set = sets.get(i.<String>getArgument(0));
            return set == null ? null : new HashSet<>(set);
        });
    }
}
