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
    final StringRedisTemplate redis=mock(StringRedisTemplate.class);
    final GradingMcpReadProgress progress=new GradingMcpReadProgress(redis);
    final UUID actor=UUID.randomUUID(),evaluation=UUID.randomUUID();
    final String fingerprint="a".repeat(64);
    @Test void missingPartialAndExpiredProgressRequireRereading() {
        var values=mock(ValueOperations.class);when(redis.opsForValue()).thenReturn(values);
        assertRequired(() -> progress.requireComplete(actor,evaluation,fingerprint,0));
        when(values.get(anyString())).thenReturn("2");
        assertRequired(() -> progress.requireComplete(actor,evaluation,fingerprint,3));
        progress.requireComplete(actor,evaluation,fingerprint,2);
        when(values.get(anyString())).thenReturn("0");
        progress.requireComplete(actor,evaluation,fingerprint,0);
    }
    @Test void gapsAreRejectedAndRedisFailureDoesNotDisableTheGuard() {
        when(redis.execute(eq(GradingMcpReadProgress.ADVANCE),anyList(),any(),any(),any())).thenReturn(-1L);
        assertRequired(() -> progress.advance(actor,evaluation,fingerprint,3,4));
        when(redis.execute(eq(GradingMcpReadProgress.ADVANCE),anyList(),any(),any(),any()))
                .thenThrow(new RedisConnectionFailureException("unavailable"));
        assertThatThrownBy(() -> progress.advance(actor,evaluation,fingerprint,0,2))
                .isInstanceOfSatisfying(ApiException.class,ex -> assertThat(ex.code()).isEqualTo("MCP_CONTEXT_READ_UNAVAILABLE"));
    }
    @Test void keyIsIsolatedByActorEvaluationAndSnapshotAndExpiryIsBounded() {
        String key=GradingMcpReadProgress.key(actor,evaluation,fingerprint);
        assertThat(key).isNotEqualTo(GradingMcpReadProgress.key(UUID.randomUUID(),evaluation,fingerprint));
        assertThat(key).isNotEqualTo(GradingMcpReadProgress.key(actor,UUID.randomUUID(),fingerprint));
        assertThat(key).isNotEqualTo(GradingMcpReadProgress.key(actor,evaluation,"b".repeat(64)));
        when(redis.execute(GradingMcpReadProgress.ADVANCE,List.of(key),"0","2","1800")).thenReturn(2L);
        progress.advance(actor,evaluation,fingerprint,0,2);
        verify(redis).execute(GradingMcpReadProgress.ADVANCE,List.of(key),"0","2","1800");
    }
    private static void assertRequired(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call).isInstanceOfSatisfying(ApiException.class,
                ex -> assertThat(ex.code()).isEqualTo("MCP_CONTEXT_READ_REQUIRED"));
    }
}
