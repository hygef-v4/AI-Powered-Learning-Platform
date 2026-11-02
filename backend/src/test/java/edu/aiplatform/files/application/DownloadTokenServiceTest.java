package edu.aiplatform.files.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class DownloadTokenServiceTest {

    @SuppressWarnings("unchecked")
    final ValueOperations<String, String> values = mock(ValueOperations.class);
    final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    final DownloadTokenService service = new DownloadTokenService(redis, new ObjectMapper());

    @Test
    void storesOnlyTheHashOfTheTokenForFiveMinutes() {
        when(redis.opsForValue()).thenReturn(values);
        UUID account = UUID.randomUUID();

        String token = service.issue("file-1", account);

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(values).set(key.capture(), anyString(), eq(DownloadTokenService.TTL));
        assertThat(token).hasSize(43);
        assertThat(key.getValue()).startsWith(DownloadTokenService.KEY_PREFIX).doesNotContain(token);
    }

    @Test
    void resolvesTheGrantFromRedis() {
        when(redis.opsForValue()).thenReturn(values);
        UUID account = UUID.randomUUID();
        when(values.get(DownloadTokenService.key("tok")))
                .thenReturn("{\"fileId\":\"file-1\",\"accountId\":\"" + account + "\"}");

        assertThat(service.resolve("tok")).contains(new DownloadTokenService.Grant("file-1", account));
        assertThat(service.resolve("khac")).isEmpty();
    }
}
