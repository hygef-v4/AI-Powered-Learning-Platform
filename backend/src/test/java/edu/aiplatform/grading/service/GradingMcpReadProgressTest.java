package edu.aiplatform.grading.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import edu.aiplatform.shared.error.ApiException;

class GradingMcpReadProgressTest {
    final StringRedisTemplate stringRedisTemplate=mock(StringRedisTemplate.class);
    final GradingMcpReadProgress progress=new GradingMcpReadProgress(stringRedisTemplate);
    final UUID actor=UUID.randomUUID(),evaluation=UUID.randomUUID();
    final String fingerprint="a".repeat(64);
    @Test void missingPartialAndExpiredProgressRequireRereading() {
        var values=mock(ValueOperations.class);when(stringRedisTemplate.opsForValue()).thenReturn(values);
        assertRequired(() -> progress.requireCompleteRead(actor,evaluation,fingerprint,0));
        when(values.get(anyString())).thenReturn("2");
        assertRequired(() -> progress.requireCompleteRead(actor,evaluation,fingerprint,3));
        progress.requireCompleteRead(actor,evaluation,fingerprint,2);
        when(values.get(anyString())).thenReturn("0");
        progress.requireCompleteRead(actor,evaluation,fingerprint,0);
    }
    @Test void gapsAreRejectedAndRedisFailureDoesNotDisableTheGuard() {
        when(stringRedisTemplate.execute(eq(GradingMcpReadProgress.ADVANCE),anyList(),any(),any(),any())).thenReturn(-1L);
        assertRequired(() -> progress.advanceProgress(actor,evaluation,fingerprint,3,4));
        when(stringRedisTemplate.execute(eq(GradingMcpReadProgress.ADVANCE),anyList(),any(),any(),any()))
                .thenThrow(new RedisConnectionFailureException("unavailable"));
        assertThatThrownBy(() -> progress.advanceProgress(actor,evaluation,fingerprint,0,2))
                .isInstanceOfSatisfying(ApiException.class,ex -> assertThat(ex.readCode()).isEqualTo("MCP_CONTEXT_READ_UNAVAILABLE"));
    }
    private static void assertRequired(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call).isInstanceOfSatisfying(ApiException.class,
                ex -> assertThat(ex.readCode()).isEqualTo("MCP_CONTEXT_READ_REQUIRED"));
    }
}
