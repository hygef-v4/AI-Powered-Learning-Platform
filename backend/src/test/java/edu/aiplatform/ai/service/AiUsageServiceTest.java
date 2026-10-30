package edu.aiplatform.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.aiplatform.ai.service.IProviderGateway.Usage;
import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.ai.contract.UsageMeasurement;
import edu.aiplatform.ai.model.AiUsageRecord;
import edu.aiplatform.ai.model.AiUsageRecord.Source;
import edu.aiplatform.ai.repository.AiUsageRepository;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.AuthorizationDecision;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AiUsageServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-21T03:00:00Z");
    private final AiUsageRepository repository = mock(AiUsageRepository.class);
    private final IAuthorization authorization = mock(IAuthorization.class);
    private final AiUsageService service = new AiUsageService(repository,
            new AiAccess(authorization, mock(IAudit.class), Clock.fixed(NOW, ZoneOffset.UTC)),
            Clock.fixed(NOW, ZoneOffset.UTC));
    private final UUID account = UUID.randomUUID();

    @Test
    void measuredUsageKeepsProviderNumbersAndUnknownIsNeverZero() {
        service.recordProvider(account, Source.PERSONAL, null, AiProvider.OPENAI, "gpt-4.1-mini", "X", "READY",
                new Usage(100L, 20L, 5L, null), null);
        service.recordProvider(account, Source.PERSONAL, null, AiProvider.OPENAI, "gpt-4.1-mini", "X", "REJECTED", null,
                null);
        service.recordProvider(account, Source.SHARED, null, AiProvider.GEMINI, "gemini-2.5-flash", "X", "UNKNOWN", null,
                700L);

        ArgumentCaptor<AiUsageRecord> records = ArgumentCaptor.forClass(AiUsageRecord.class);
        verify(repository, org.mockito.Mockito.times(3)).insert(records.capture());
        List<AiUsageRecord> all = records.getAllValues();
        assertThat(all.get(0).provenance()).isEqualTo(UsageMeasurement.PROVIDER_MEASURED);
        assertThat(all.get(0).cachedTokens()).isEqualTo(5);
        assertThat(all.get(1).provenance()).isEqualTo(UsageMeasurement.UNAVAILABLE);
        assertThat(all.get(1).inputTokens()).isNull();
        assertThat(all.get(1).outputTokens()).isNull();
        assertThat(all.get(2).provenance()).isEqualTo(UsageMeasurement.ESTIMATED);
        assertThat(all.get(2).outputTokens()).isEqualTo(700);
    }

    @Test
    void mcpUsageIsExternallyReportedOnlyWhenTheAgentSendsNumbers() {
        service.recordMcp(account, UUID.randomUUID(), "read_own_work", "OK", null, null);
        service.recordMcp(account, UUID.randomUUID(), "read_own_work", "OK", 1200L, 300L);

        ArgumentCaptor<AiUsageRecord> records = ArgumentCaptor.forClass(AiUsageRecord.class);
        verify(repository, org.mockito.Mockito.times(2)).insert(records.capture());
        assertThat(records.getAllValues().get(0).provenance()).isEqualTo(UsageMeasurement.UNAVAILABLE);
        assertThat(records.getAllValues().get(0).inputTokens()).isNull();
        assertThat(records.getAllValues().get(1).provenance()).isEqualTo(UsageMeasurement.EXTERNALLY_REPORTED);
    }

    @Test
    void historyIsOwnOnlyPagedAndValidated() {
        Actor student = new Actor(account, Role.STUDENT);
        when(authorization.authorize(eq(student), any(), any())).thenReturn(AuthorizationDecision.allow());
        when(repository.countOwn(account, null, null, null)).thenReturn(1L);
        when(repository.totalsOwn(account, null, null)).thenReturn(Map.of(Source.MCP, new long[] {4, 0}));

        var page = service.listOwn(student, null, null, null, 0, 50);

        verify(repository).listOwn(eq(account), eq(null), eq(null), eq(null), eq(0), eq(50));
        assertThat(page.mcp()).containsExactly(4, 0);
        assertThat(page.shared()).containsExactly(0, 0);
        assertThatThrownBy(() -> service.listOwn(student, null, null, null, 0, 101))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("AI_USAGE_PAGE_INVALID"));
        assertThatThrownBy(() -> service.listOwn(student, null, NOW, NOW.minusSeconds(1), 0, 10))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("AI_USAGE_RANGE_INVALID"));
        Actor admin = new Actor(UUID.randomUUID(), Role.ADMIN);
        when(authorization.authorize(eq(admin), any(), any())).thenReturn(AuthorizationDecision.deny("ROLE"));
        assertThatThrownBy(() -> service.listOwn(admin, null, null, null, 0, 10)).isInstanceOf(ApiException.class);
        verify(repository, org.mockito.Mockito.never()).listOwn(eq(admin.accountId()), any(), any(), any(), anyInt(), anyInt());
    }
}
