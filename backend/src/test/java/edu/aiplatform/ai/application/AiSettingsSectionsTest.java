package edu.aiplatform.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.ai.domain.AiConnection;
import edu.aiplatform.ai.domain.AiConnectionRepository;
import edu.aiplatform.ai.domain.QuotaPolicy;
import edu.aiplatform.ai.domain.QuotaRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AiSettingsSectionsTest {

    private static final Instant NOW = Instant.parse("2026-10-21T03:00:00Z");

    @Test
    void connectionsSummaryShowsStateAndModelButNoKey() {
        AiConnectionRepository repository = mock(AiConnectionRepository.class);
        AiConnectionsSection section = new AiConnectionsSection(repository);
        when(repository.listInstitutional()).thenReturn(List.of());
        assertThat(section.summary()).isEqualTo("Chưa cấu hình");

        AiConnection gemini = AiConnection.create(UUID.randomUUID(), AiConnection.Scope.INSTITUTIONAL, null,
                AiProvider.GEMINI, "gemini-2.5-flash", new AiConnection.EncryptedKey(new byte[] {1}, new byte[] {1}, 1),
                "Zx9q", NOW);
        when(repository.listInstitutional()).thenReturn(List.of(gemini));

        assertThat(section.sectionKey()).isEqualTo("ai-connections");
        assertThat(section.summary()).isEqualTo(
                "Gemini: đang bật (gemini-2.5-flash) · OpenAI: chưa cấu hình · Claude: chưa cấu hình")
                .doesNotContain("Zx9q");
    }

    @Test
    void quotaSummaryShowsLimitsAndCustomCount() {
        QuotaRepository repository = mock(QuotaRepository.class);
        TeacherQuotaSection section = new TeacherQuotaSection(repository);
        when(repository.policy()).thenReturn(QuotaPolicy.empty());
        assertThat(section.summary()).isEqualTo("Chưa cấu hình");

        when(repository.policy()).thenReturn(new QuotaPolicy(20_000L, 500_000L, 1, null, NOW));
        when(repository.listTeacherQuotas()).thenReturn(List.of(
                new QuotaPolicy.TeacherQuota(UUID.randomUUID(), 50_000, 0, null, NOW)));

        assertThat(section.sectionKey()).isEqualTo("teacher-quota");
        assertThat(section.summary()).isEqualTo(
                "Mặc định 20.000 token/ngày · trần chung 500.000 token/ngày · 1 giảng viên có mức riêng");
    }
}
