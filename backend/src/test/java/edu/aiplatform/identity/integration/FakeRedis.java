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
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

/** {@link StringRedisTemplate} giả trong bộ nhớ cho các lệnh U01 dùng; ghi lại TTL để test kiểm. */
final class FakeRedis {

    final Map<String, String> values = new HashMap<>();
    final Map<String, Duration> ttls = new HashMap<>();
    final Map<String, Set<String>> sets = new HashMap<>();
    final Map<String, Map<Object, Object>> hashes = new HashMap<>();
    final StringRedisTemplate template = mock(StringRedisTemplate.class);

    @SuppressWarnings("unchecked")
    FakeRedis() {
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

        // SessionStore.CLAIM_REFRESH: đọc refresh token và đánh dấu "used:{sid}:{millis}" trong một bước.
        when(template.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenAnswer(i -> {
            String key = i.<List<String>>getArgument(1).get(0);
            String value = values.get(key);
            if (value == null || value.startsWith(SessionStore.USED_MARK)) {
                return value;
            }
            values.put(key, SessionStore.USED_MARK + value + ":" + i.getArgument(2));
            ttls.put(key, Duration.ofMillis(Long.parseLong(i.getArgument(3))));
            return value;
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
